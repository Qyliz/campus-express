package cn.njust.campusexpress.order;

import cn.njust.campusexpress.TestPhones;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.entity.*;
import cn.njust.campusexpress.model.order.mapper.*;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static cn.njust.campusexpress.common.enums.UserRoleEnum.*;
import static cn.njust.campusexpress.common.enums.OrderStatusEnum.*;
import static cn.njust.campusexpress.common.enums.PaymentStatusEnum.PAID;
import static cn.njust.campusexpress.common.enums.PaymentStatusEnum.REFUNDED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecipientExceptionTest {
    @Autowired OrderService service;
    @Autowired UserService users;
    @Autowired RoleAccountService accounts;
    @Autowired ExpressOrderMapper orders;
    @MockitoSpyBean DeliveryExceptionMapper exceptions;
    @Autowired JdbcTemplate jdbc;
    @Autowired Validator validator;
    @Autowired MockMvc mvc;
    final List<Long> userIds = new ArrayList<>();
    final List<Long> orderIds = new ArrayList<>();
    Long sender, recipient, courier, admin, stranger;
    /** 收件联系方式：订单只按手机号匹配，本字段承载当前用于认领的手机号。 */
    String phone;

    Long user(UserRoleEnum role) {
        User u = new User();
        u.setUsername("异常测试"); u.setGender(UserGenderEnum.UNKNOWN);
        u.setPhone(TestPhones.next());
        u.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("test123", org.mindrot.jbcrypt.BCrypt.gensalt()));
        u.setEmail(UUID.randomUUID() + "@example.com");
        users.save(u); userIds.add(u.getId());
        accounts.createAccount(u.getId(), role, UserStatusEnum.NORMAL);
        return u.getId();
    }
    @BeforeEach void setup() {
        sender = user(CUSTOMER); recipient = user(CUSTOMER); courier = user(COURIER); admin = user(ADMIN); stranger = user(CUSTOMER);
        phone = users.getById(recipient).getPhone();
    }
    @AfterEach void cleanup() {
        reset(exceptions);
        for (Long id : orderIds) {
            jdbc.update("delete from delivery_exception where order_id = ?", id);
            jdbc.update("delete from order_status_record where order_id = ?", id);
            jdbc.update("delete from express_order where id = ?", id);
        }
        for (Long id : userIds) {
            for (String table : List.of("customer", "courier", "admin")) jdbc.update("delete from " + table + " where user_id = ?", id);
            jdbc.update("delete from user where id = ?", id);
        }
    }
    CreateOrderDTO form() {
        CreateOrderDTO d = new CreateOrderDTO();
        d.setPickupAddress("站点"); d.setPickupName("寄件人"); d.setPickupPhone("13800000001");
        d.setDeliveryAddress("宿舍"); d.setDeliveryName("收件人"); d.setDeliveryPhone(phone);
        d.setItemDescription("书籍"); d.setFee(new BigDecimal("5.00")); return d;
    }
    Long create(CreateOrderDTO d) { Long id = service.create(sender, CUSTOMER, d); orderIds.add(id); return id; }
    Long accepted() {
        Long id = create(form()); service.act(sender, CUSTOMER, id, "pay", null);
        service.act(courier, COURIER, id, "accept", null); return id;
    }
    ReportExceptionDTO report() { var d = new ReportExceptionDTO(); d.setType(ExceptionTypeEnum.CONTACT); d.setDescription("联系不上收件人"); return d; }
    ResolveExceptionDTO resolution(ExceptionResolutionEnum value) { var d = new ResolveExceptionDTO(); d.setResolution(value); d.setDescription("已与双方协商"); return d; }
    Cookie login(Long id, UserRoleEnum role) throws Exception {
        return mvc.perform(post("/api/user/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"account\":\"" + users.getById(id).getEmail() + "\",\"password\":\"test123\",\"role\":\"" + role + "\"}"))
            .andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getCookie("satoken");
    }

    @Test void recipientCanReadAndCompleteButCannotPayOrCancel() {
        Long id = create(form());
        assertEquals(phone, orders.selectById(id).getDeliveryPhone());
        assertTrue(service.detail(recipient, CUSTOMER, id).allowedActions().isEmpty());
        assertEquals(1, service.list(recipient, CUSTOMER, "mine", new OrderQueryDTO()).getTotal());
        assertThrows(BusinessException.class, () -> service.act(recipient, CUSTOMER, id, "pay", null));
        assertThrows(BusinessException.class, () -> service.act(recipient, CUSTOMER, id, "cancel", "取消"));
        assertThrows(BusinessException.class, () -> service.detail(stranger, CUSTOMER, id));
        service.act(sender, CUSTOMER, id, "pay", null); service.act(courier, COURIER, id, "accept", null);
        service.act(courier, COURIER, id, "pickup", null); service.act(courier, COURIER, id, "deliver", null);
        assertEquals(List.of("complete"), service.detail(recipient, CUSTOMER, id).allowedActions());
        service.act(recipient, CUSTOMER, id, "complete", null);
        assertEquals(COMPLETED, orders.selectById(id).getOrderStatus());
    }

    @Test void dynamicMatchingDeduplicatesAndRespectsPaginationAndContactChanges() {
        phone = users.getById(sender).getPhone();
        Long id = create(form());
        var q = new OrderQueryDTO();
        assertEquals(1, service.list(sender, CUSTOMER, "mine", q).getTotal());
        var row = service.list(sender, CUSTOMER, "mine", q).getRecords().get(0);
        assertTrue(row.isCreatedByMe()); assertTrue(row.isReceivedByMe());
        q.setRelation("received"); assertEquals(1, service.list(sender, CUSTOMER, "mine", q).getTotal());
        phone = TestPhones.next();
        Long later = create(form()); // 尚无对应账户，仍能下单。
        assertThrows(BusinessException.class, () -> service.detail(recipient, CUSTOMER, later));
        jdbc.update("update user set phone = ? where id = ?", phone, recipient);
        assertTrue(service.detail(recipient, CUSTOMER, later).order().isReceivedByMe());
        for (int i=0;i<11;i++) create(form());
        q.setCurrentPage(2);
        assertEquals(12, service.list(recipient, CUSTOMER, "mine", q).getTotal());
        assertEquals(2, service.list(recipient, CUSTOMER, "mine", q).getRecords().size());
        jdbc.update("update user set phone = ? where id = ?", TestPhones.next(), recipient);
        assertThrows(BusinessException.class, () -> service.detail(recipient, CUSTOMER, later));
        assertEquals(id, service.detail(sender, CUSTOMER, id).order().getId());
    }

    @Test void phoneMatchingWorksAndBlankContactNeverMatches() {
        jdbc.update("update user set phone = ? where id = ?", "13899998888", stranger);
        var d = form(); d.setDeliveryPhone("13899998888"); Long id = create(d);
        assertTrue(service.detail(stranger, CUSTOMER, id).order().isReceivedByMe());
        // 这张单的收件手机号属于 stranger，recipient 已经没有任何通道可以认领。
        assertThrows(BusinessException.class, () -> service.detail(recipient, CUSTOMER, id));
        // 空手机号永不参与匹配。置成空串而不是 null：phone 是 NOT NULL，而 recipientQuery 的 isBlank 守卫正好覆盖这一支。
        jdbc.update("update user set phone = '' where id = ?", stranger);
        assertEquals(0, service.list(stranger, CUSTOMER, "mine", new OrderQueryDTO()).getTotal());
        assertThrows(BusinessException.class, () -> service.detail(stranger, CUSTOMER, id));
    }

    @Test void reportPausesBothStagesAndResumePreservesHistory() {
        Long id = accepted();
        Long first = service.reportException(courier, COURIER, id, report());
        assertThrows(BusinessException.class, () -> service.reportException(courier, COURIER, id, report()));
        assertThrows(BusinessException.class, () -> service.act(courier, COURIER, id, "pickup", null));
        assertTrue(service.detail(recipient, CUSTOMER, id).order().isPendingException());
        assertTrue(service.list(recipient, CUSTOMER, "mine", new OrderQueryDTO()).getRecords().get(0).isPendingException());
        assertEquals(first, service.exceptionDetail(recipient, CUSTOMER, first).getId());
        assertThrows(BusinessException.class, () -> service.exceptionDetail(stranger, CUSTOMER, first));
        assertThrows(BusinessException.class, () -> service.resolveException(courier, COURIER, first, resolution(ExceptionResolutionEnum.RESUME)));
        service.resolveException(admin, ADMIN, first, resolution(ExceptionResolutionEnum.RESUME));
        assertEquals(AWAITING_PICKUP, orders.selectById(id).getOrderStatus());
        assertFalse(service.detail(courier, COURIER, id).order().isPendingException());
        service.act(courier, COURIER, id, "pickup", null);
        Long second = service.reportException(courier, COURIER, id, report());
        assertThrows(BusinessException.class, () -> service.act(courier, COURIER, id, "deliver", null));
        service.resolveException(admin, ADMIN, second, resolution(ExceptionResolutionEnum.RESUME));
        assertEquals(DELIVERING, orders.selectById(id).getOrderStatus());
        assertEquals(2, service.detail(sender, CUSTOMER, id).exceptions().size());
        assertThrows(BusinessException.class, () -> service.resolveException(admin, ADMIN, second, resolution(ExceptionResolutionEnum.CANCEL)));
    }

    @Test void bothCancellationEntrypointsCloseExceptionAndRefund() {
        for (boolean direct : List.of(false, true)) {
            Long id = accepted(); Long exception = service.reportException(courier, COURIER, id, report());
            if (direct) service.act(admin, ADMIN, id, "admin-cancel", "无法配送");
            else service.resolveException(admin, ADMIN, exception, resolution(ExceptionResolutionEnum.CANCEL));
            assertEquals(CANCELLED, orders.selectById(id).getOrderStatus());
            assertEquals(REFUNDED, orders.selectById(id).getPaymentStatus());
            assertEquals(ExceptionStatusEnum.RESOLVED, exceptions.selectById(exception).getStatus());
            assertEquals(ExceptionResolutionEnum.CANCEL, exceptions.selectById(exception).getResolution());
            assertThrows(BusinessException.class, () -> service.reportException(courier, COURIER, id, report()));
        }
    }

    @Test void saveFailuresRollbackOrderVersionRefundAndException() {
        Long id = accepted(); int version = orders.selectById(id).getVersion();
        doThrow(new IllegalStateException("模拟保存失败")).when(exceptions).insert(any(DeliveryException.class));
        assertThrows(IllegalStateException.class, () -> service.reportException(courier, COURIER, id, report()));
        assertEquals(version, orders.selectById(id).getVersion());
        assertTrue(service.detail(sender, CUSTOMER, id).exceptions().isEmpty());
        reset(exceptions);
        Long exception = service.reportException(courier, COURIER, id, report());
        doThrow(new IllegalStateException("模拟处理保存失败")).when(exceptions).updateById(any(DeliveryException.class));
        assertThrows(IllegalStateException.class, () -> service.resolveException(admin, ADMIN, exception, resolution(ExceptionResolutionEnum.CANCEL)));
        assertEquals(AWAITING_PICKUP, orders.selectById(id).getOrderStatus());
        assertEquals(PAID, orders.selectById(id).getPaymentStatus());
        assertEquals(ExceptionStatusEnum.PENDING, exceptions.selectById(exception).getStatus());
        assertEquals(version + 1, orders.selectById(id).getVersion());
    }

    int race(Runnable a, Runnable b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (Runnable action : List.of(a,b)) results.add(pool.submit(() -> {
                start.await();
                try { action.run(); return true; } catch (BusinessException e) { return false; }
            }));
            start.countDown(); int successes = 0;
            for (Future<Boolean> result : results) if (result.get(15, TimeUnit.SECONDS)) successes++;
            return successes;
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentReportDeliveryResolutionAndCompletionStayConsistent() throws Exception {
        Long id = accepted(); service.act(courier, COURIER, id, "pickup", null);
        assertEquals(1, race(() -> service.reportException(courier, COURIER, id, report()),
            () -> service.act(courier, COURIER, id, "deliver", null)));
        var detail = service.detail(sender, CUSTOMER, id);
        if (detail.order().isPendingException()) {
            Long exception = detail.exceptions().get(0).getId();
            Long secondAdmin = user(ADMIN);
            assertEquals(1, race(() -> service.resolveException(admin, ADMIN, exception, resolution(ExceptionResolutionEnum.RESUME)),
                () -> service.resolveException(secondAdmin, ADMIN, exception, resolution(ExceptionResolutionEnum.RESUME))));
            service.act(courier, COURIER, id, "deliver", null);
        }
        assertEquals(1, race(() -> service.act(sender, CUSTOMER, id, "complete", null),
            () -> service.act(recipient, CUSTOMER, id, "complete", null)));
        Long another = accepted();
        assertEquals(1, race(() -> service.reportException(courier, COURIER, another, report()),
            () -> service.reportException(courier, COURIER, another, report())));
        assertEquals(1, service.detail(sender, CUSTOMER, another).exceptions().size());
    }

    @Test void requestValidationAndHttpAuthorization() throws Exception {
        var d = form(); d.setDeliveryPhone(" ");
        assertFalse(validator.validate(d).isEmpty()); // 空串被 setter 归一成 null，由 @NotBlank 稳定拒绝
        d.setDeliveryPhone(phone); assertTrue(validator.validate(d).isEmpty());
        Long id = accepted(); Cookie c = login(courier, COURIER);
        mvc.perform(post("/api/order/{id}/exceptions", id).cookie(c).contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"BAD\",\"description\":\"x\"}")).andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        mvc.perform(post("/api/order/{id}/exceptions", id).cookie(c).contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"OTHER\",\"description\":\" \"}")).andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        Long exception = service.reportException(courier, COURIER, id, report());
        Cookie r = login(recipient, CUSTOMER);
        mvc.perform(get("/api/exception/{id}", exception).cookie(r)).andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/api/exception/{id}/resolve", exception).cookie(r).contentType(MediaType.APPLICATION_JSON)
            .content("{\"resolution\":\"RESUME\",\"description\":\"x\"}")).andExpect(jsonPath("$.code").value(ResultCodeEnum.NO_PERMISSION.getCode()));
        mvc.perform(get("/api/exception/admin").cookie(r)).andExpect(jsonPath("$.code").value(ResultCodeEnum.NO_PERMISSION.getCode()));
        mvc.perform(get("/api/order/{id}", id).cookie(r)).andExpect(jsonPath("$.data.allowedActions").isEmpty());
        Cookie a = login(admin, ADMIN);
        mvc.perform(post("/api/user/{id}/kickout", sender).cookie(r)).andExpect(status().isForbidden());
        mvc.perform(post("/api/user/not-a-number/kickout").cookie(a))
            .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        mvc.perform(post("/api/user/{id}/roles/INVALID/unban", sender).cookie(a))
            .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        mvc.perform(post("/api/exception/{id}/resolve", exception).cookie(a).contentType(MediaType.APPLICATION_JSON)
            .content("{\"resolution\":\"BAD\",\"description\":\"x\"}")).andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    @Test void wrongCourierInvalidStageAndOversizedReportsAreRejected() {
        Long id = create(form());
        assertThrows(BusinessException.class, () -> service.reportException(courier, COURIER, id, report()));
        service.act(sender, CUSTOMER, id, "pay", null);
        assertThrows(BusinessException.class, () -> service.reportException(courier, COURIER, id, report()));
        service.act(courier, COURIER, id, "accept", null);
        Long otherCourier = user(COURIER);
        assertThrows(BusinessException.class, () -> service.reportException(otherCourier, COURIER, id, report()));
        var tooLong = report(); tooLong.setDescription("x".repeat(256));
        assertThrows(BusinessException.class, () -> service.reportException(courier, COURIER, id, tooLong));
        service.act(courier, COURIER, id, "pickup", null); service.act(courier, COURIER, id, "deliver", null);
        assertThrows(BusinessException.class, () -> service.reportException(courier, COURIER, id, report()));
        service.act(recipient, CUSTOMER, id, "complete", null);
        assertThrows(BusinessException.class, () -> service.reportException(courier, COURIER, id, report()));
    }
}
