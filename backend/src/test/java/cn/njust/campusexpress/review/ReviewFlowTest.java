package cn.njust.campusexpress.review;

import cn.njust.campusexpress.TestPhones;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.review.dto.*;
import cn.njust.campusexpress.model.review.entity.ReviewAppeal;
import cn.njust.campusexpress.model.review.mapper.*;
import cn.njust.campusexpress.model.review.service.ReviewService;
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
import org.mindrot.jbcrypt.BCrypt;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.*;
import static cn.njust.campusexpress.common.enums.UserRoleEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewFlowTest {
    @Autowired ReviewService service;
    @Autowired OrderService orders;
    @Autowired UserService users;
    @Autowired RoleAccountService accounts;
    @Autowired ServiceReviewMapper reviews;
    @MockitoSpyBean ReviewAppealMapper appeals;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired tools.jackson.databind.json.JsonMapper json;
    final List<Long> userIds = new ArrayList<>();
    final List<Long> orderIds = new ArrayList<>();
    Long sender, recipient, courier, otherCourier, admin, stranger;

    Long user(UserRoleEnum role) {
        var user = new User();
        user.setUsername("评价测试");
        user.setGender(UserGenderEnum.UNKNOWN);
        user.setPhone(TestPhones.next());
        user.setEmail(UUID.randomUUID() + "@review.test");
        user.setPassword(BCrypt.hashpw("test123", BCrypt.gensalt(4)));
        users.save(user);
        userIds.add(user.getId());
        accounts.createAccount(user.getId(), role, UserStatusEnum.NORMAL);
        return user.getId();
    }
    @BeforeEach void setup() {
        sender = user(CUSTOMER); recipient = user(CUSTOMER); courier = user(COURIER);
        otherCourier = user(COURIER); admin = user(ADMIN); stranger = user(CUSTOMER);
    }
    @AfterEach void cleanup() {
        reset(appeals);
        for (var id : orderIds) {
            jdbc.update("delete from delivery_exception where order_id = ?", id);
            jdbc.update("delete from review_appeal where order_id = ?", id);
            jdbc.update("delete from service_review where order_id = ?", id);
            jdbc.update("delete from order_status_record where order_id = ?", id);
            jdbc.update("delete from express_order where id = ?", id);
        }
        for (var id : userIds) {
            jdbc.update("delete from customer where user_id = ?", id);
            jdbc.update("delete from courier where user_id = ?", id);
            jdbc.update("delete from admin where user_id = ?", id);
            jdbc.update("delete from user where id = ?", id);
        }
    }
    Long order(Long receiver, boolean complete) {
        var dto = new CreateOrderDTO();
        dto.setPickupAddress("快递站"); dto.setPickupName("寄件人"); dto.setPickupPhone("13800000001");
        dto.setDeliveryAddress("宿舍"); dto.setDeliveryName("收件人");
        dto.setDeliveryPhone(users.getById(receiver).getPhone());
        dto.setItemDescription("书籍"); dto.setFee(new BigDecimal("5.00"));
        var id = orders.create(sender, CUSTOMER, dto);
        orderIds.add(id);
        if (complete) {
            orders.act(sender, CUSTOMER, id, "pay", null);
            orders.act(courier, COURIER, id, "accept", null);
            orders.act(courier, COURIER, id, "pickup", null);
            orders.act(courier, COURIER, id, "deliver", null);
            orders.act(receiver, CUSTOMER, id, "complete", null);
        }
        return id;
    }
    Long review(Long orderId) { return service.create(sender, CUSTOMER, orderId, new ReviewDTO(4, " 服务及时 ")); }
    Cookie login(Long id, UserRoleEnum role) throws Exception {
        return mvc.perform(post("/api/user/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"account\":\"" + users.getById(id).getEmail() + "\",\"password\":\"test123\",\"role\":\"" + role + "\"}"))
            .andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getCookie("satoken");
    }

    @Test void bothParticipantsCanReviewAndSamePersonOnlyOnce() {
        var id = order(recipient, true);
        assertTrue(service.list(sender, CUSTOMER, id).canReview());
        review(id);
        service.create(recipient, CUSTOMER, id, new ReviewDTO(5, "满意"));
        assertEquals(2, service.list(courier, COURIER, id).reviews().size());
        assertFalse(service.list(sender, CUSTOMER, id).canReview());
        assertThrows(BusinessException.class, () -> review(id));
        var own = order(sender, true);
        review(own);
        assertThrows(BusinessException.class, () -> review(own));
        assertEquals(1, service.list(sender, CUSTOMER, own).reviews().size());
    }

    @Test void invalidStageAndUnauthorizedRolesCannotReadOrWrite() {
        var unfinished = order(recipient, false);
        assertThrows(BusinessException.class, () -> review(unfinished));
        orders.act(sender, CUSTOMER, unfinished, "cancel", "取消");
        assertThrows(BusinessException.class, () -> review(unfinished));
        var id = order(recipient, true);
        assertThrows(BusinessException.class, () -> service.create(stranger, CUSTOMER, id, new ReviewDTO(4, "评价")));
        assertThrows(BusinessException.class, () -> service.list(stranger, CUSTOMER, id));
        assertThrows(BusinessException.class, () -> service.list(otherCourier, COURIER, id));
        assertThrows(BusinessException.class, () -> service.create(courier, COURIER, id, new ReviewDTO(4, "评价")));
        var reviewId = review(id);
        assertThrows(BusinessException.class, () -> service.appeal(otherCourier, COURIER, reviewId, new AppealDTO("理由")));
        assertThrows(BusinessException.class, () -> service.appeal(sender, CUSTOMER, reviewId, new AppealDTO("理由")));
        var appealId = service.appeal(courier, COURIER, reviewId, new AppealDTO("理由"));
        assertThrows(BusinessException.class, () -> service.resolve(sender, CUSTOMER, appealId, new ResolveAppealDTO(AppealStatusEnum.UPHELD, "理由")));
        assertThrows(BusinessException.class, () -> service.adminList(courier, COURIER, new AppealQueryDTO()));
        var account = accounts.getByUserAndRole(sender, CUSTOMER);
        accounts.updateStatus(account, CUSTOMER, UserStatusEnum.DISABLED);
        assertThrows(BusinessException.class, () -> service.list(sender, CUSTOMER, id));
    }

    @Test void upheldAndRejectedAppealsRetainHistoryAndCannotBeRepeated() {
        var id = order(recipient, true);
        var first = review(id);
        var second = service.create(recipient, CUSTOMER, id, new ReviewDTO(2, "送达较晚"));
        var a = service.appeal(courier, COURIER, first, new AppealDTO(" 已及时送达 "));
        var b = service.appeal(courier, COURIER, second, new AppealDTO("解释"));
        assertThrows(BusinessException.class, () -> service.appeal(courier, COURIER, first, new AppealDTO("重复")));
        service.resolve(admin, ADMIN, a, new ResolveAppealDTO(AppealStatusEnum.UPHELD, " 核实成立 "));
        service.resolve(admin, ADMIN, b, new ResolveAppealDTO(AppealStatusEnum.REJECTED, "评价合理"));
        assertEquals(ReviewStatusEnum.VOID, reviews.selectById(first).getStatus());
        assertEquals(ReviewStatusEnum.VALID, reviews.selectById(second).getStatus());
        assertEquals("服务及时", reviews.selectById(first).getContent());
        assertEquals("核实成立", appeals.selectById(a).getResolutionReason());
        assertNotNull(appeals.selectById(a).getResolvedTime());
        assertEquals(accounts.getByUserAndRole(admin, ADMIN).getId(), appeals.selectById(a).getAdminId());
        assertThrows(BusinessException.class, () -> service.resolve(admin, ADMIN, a, new ResolveAppealDTO(AppealStatusEnum.REJECTED, "再处理")));
        assertThrows(BusinessException.class, () -> service.appeal(courier, COURIER, first, new AppealDTO("作废后")));
        assertThrows(BusinessException.class, () -> service.appeal(courier, COURIER, second, new AppealDTO("驳回后")));
        assertTrue(service.list(courier, COURIER, id).reviews().stream().noneMatch(r -> r.canAppeal()));
        assertEquals(2, service.list(admin, ADMIN, id).reviews().size());
        var query = new AppealQueryDTO(); query.setOrderId(id);
        assertEquals(2, service.adminList(admin, ADMIN, query).getTotal());
        query.setStatus(AppealStatusEnum.PENDING); assertEquals(0, service.adminList(admin, ADMIN, query).getTotal());
        query.setStatus(AppealStatusEnum.UPHELD); assertEquals(1, service.adminList(admin, ADMIN, query).getTotal());
        query.setStatus(null); assertEquals(2, service.adminList(admin, ADMIN, query).getTotal());
    }

    @Test void validationCoversRatingsWhitespaceAndLength() {
        var id = order(recipient, true);
        for (Integer rating : Arrays.asList(null, 0, 6)) {
            assertThrows(BusinessException.class, () -> service.create(sender, CUSTOMER, id, new ReviewDTO(rating, "内容")));
        }
        for (String content : Arrays.asList(null, " \n ", "字".repeat(501))) {
            assertThrows(BusinessException.class, () -> service.create(sender, CUSTOMER, id, new ReviewDTO(3, content)));
        }
        var reviewId = service.create(sender, CUSTOMER, id, new ReviewDTO(1, "字".repeat(500)));
        for (String reason : Arrays.asList(null, " ", "字".repeat(501))) {
            assertThrows(BusinessException.class, () -> service.appeal(courier, COURIER, reviewId, new AppealDTO(reason)));
        }
        var appealId = service.appeal(courier, COURIER, reviewId, new AppealDTO("字".repeat(500)));
        for (String reason : Arrays.asList(null, " ", "字".repeat(501))) {
            assertThrows(BusinessException.class, () -> service.resolve(admin, ADMIN, appealId, new ResolveAppealDTO(AppealStatusEnum.UPHELD, reason)));
        }
        // 原来由 @Min(1) 挡住「改回待处理」，status 换成枚举后这条规则挪到了 ReviewServiceImpl.resolve 里。
        assertThrows(BusinessException.class, () -> service.resolve(admin, ADMIN, appealId, new ResolveAppealDTO(AppealStatusEnum.PENDING, "理由")));
        assertEquals(AppealStatusEnum.PENDING, appeals.selectById(appealId).getStatus());
    }

    @Test void failedResolutionRollsBackReviewInvalidation() {
        var id = order(recipient, true);
        var reviewId = review(id);
        var appealId = service.appeal(courier, COURIER, reviewId, new AppealDTO("理由"));
        doThrow(new IllegalStateException("模拟保存失败")).when(appeals).updateById(any(ReviewAppeal.class));
        assertThrows(IllegalStateException.class, () -> service.resolve(admin, ADMIN, appealId, new ResolveAppealDTO(AppealStatusEnum.UPHELD, "成立")));
        assertEquals(ReviewStatusEnum.VALID, reviews.selectById(reviewId).getStatus());
        assertEquals(AppealStatusEnum.PENDING, appeals.selectById(appealId).getStatus());
        assertNull(appeals.selectById(appealId).getAdminId());
    }

    @Test void orderEnumsUseNamesOverHttpAndNumericDatabaseValues() throws Exception {
        for (var value : OrderStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), OrderStatusEnum.class));
        }
        for (var value : PaymentStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), PaymentStatusEnum.class));
        }
        for (var value : ExceptionStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), ExceptionStatusEnum.class));
        }
        for (var value : ExceptionTypeEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), ExceptionTypeEnum.class));
        }
        for (var value : ExceptionResolutionEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), ExceptionResolutionEnum.class));
        }
        for (var value : AppealStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), AppealStatusEnum.class));
        }
        for (var value : ReviewStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), ReviewStatusEnum.class));
        }
        var id = order(recipient, true);
        var detail = orders.detail(sender, CUSTOMER, id);
        assertEquals(OrderStatusEnum.COMPLETED, detail.order().getOrderStatus());
        assertEquals(PaymentStatusEnum.PAID, detail.order().getPaymentStatus());
        assertNull(detail.records().get(0).getFromStatus());
        assertEquals(OrderStatusEnum.UNPAID, detail.records().get(0).getToStatus());
        assertEquals(OrderStatusEnum.AWAITING_COLLECTION, detail.records().get(5).getFromStatus());
        assertEquals(OrderStatusEnum.COMPLETED, detail.records().get(5).getToStatus());
        assertEquals(5, jdbc.queryForObject("select order_status from express_order where id=?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("select payment_status from express_order where id=?", Integer.class, id));
        assertEquals(List.of(0, 1, 2, 3, 4, 5), jdbc.queryForList(
            "select to_status from order_status_record where order_id=? order by create_time,id", Integer.class, id));
        var cookie = login(sender, CUSTOMER);
        mvc.perform(get("/api/order/{id}", id).cookie(cookie))
            .andExpect(jsonPath("$.data.order.orderStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.data.order.paymentStatus").value("PAID"))
            .andExpect(jsonPath("$.data.records[0].toStatus").value("UNPAID"))
            .andExpect(jsonPath("$.data.records[5].fromStatus").value("AWAITING_COLLECTION"))
            .andExpect(jsonPath("$.data.records[5].toStatus").value("COMPLETED"));
        mvc.perform(get("/api/order/mine").cookie(cookie).param("orderStatus", "COMPLETED"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].orderStatus").value("COMPLETED"));
    }

    // 异常的 type / resolution 两列本次从 VARCHAR 改成了 TINYINT：库里必须真的存数字，接口必须真的吐枚举名。
    @Test void exceptionEnumsPersistNumericCodesAndExposeNamesOverHttp() throws Exception {
        var id = order(recipient, false);
        orders.act(sender, CUSTOMER, id, "pay", null);
        orders.act(courier, COURIER, id, "accept", null);
        orders.act(courier, COURIER, id, "pickup", null);
        var report = new ReportExceptionDTO();
        report.setType(ExceptionTypeEnum.ADDRESS);
        report.setDescription("门禁需要刷卡，无法进入");
        var exceptionId = orders.reportException(courier, COURIER, id, report);
        var resolve = new ResolveExceptionDTO();
        resolve.setResolution(ExceptionResolutionEnum.RESUME);
        resolve.setDescription("已联系收件人下楼取件");
        orders.resolveException(admin, ADMIN, exceptionId, resolve);
        // ADDRESS=1、RESUME=0、RESOLVED=1，与各枚举里声明的 code 一一对应。
        assertEquals(1, jdbc.queryForObject("select type from delivery_exception where id=?", Integer.class, exceptionId));
        assertEquals(0, jdbc.queryForObject("select resolution from delivery_exception where id=?", Integer.class, exceptionId));
        assertEquals(1, jdbc.queryForObject("select status from delivery_exception where id=?", Integer.class, exceptionId));
        mvc.perform(get("/api/order/{id}", id).cookie(login(sender, CUSTOMER)))
            .andExpect(jsonPath("$.data.exceptions[0].type").value("ADDRESS"))
            .andExpect(jsonPath("$.data.exceptions[0].resolution").value("RESUME"))
            .andExpect(jsonPath("$.data.exceptions[0].status").value("RESOLVED"));
    }

    @Test void orderFilterRejectsNumericAndUnknownEnumNames() throws Exception {
        var cookie = login(sender, CUSTOMER);
        for (String value : List.of("5", "INVALID", "completed")) {
            mvc.perform(get("/api/order/mine").cookie(cookie).param("orderStatus", value))
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        }
        // 状态筛选参数换成枚举之后，旧的数字写法必须被拒绝，否则无法确认前后端契约真的同步换掉了。
        var adminCookie = login(admin, ADMIN);
        for (String value : List.of("0", "INVALID")) {
            mvc.perform(get("/api/exception/admin").cookie(adminCookie).param("status", value))
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
            mvc.perform(get("/api/review/appeals/admin").cookie(adminCookie).param("status", value))
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        }
    }

    @Test void httpFlowIncludesLoginValidationAndRoleBoundaries() throws Exception {
        var id = order(recipient, true);
        mvc.perform(get("/api/order/{id}/reviews", id)).andExpect(status().isUnauthorized());
        var customerCookie = login(sender, CUSTOMER);
        mvc.perform(post("/api/order/{id}/reviews", id).cookie(customerCookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"rating\":6,\"content\":\"评价\"}")).andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        mvc.perform(post("/api/order/{id}/reviews", id).cookie(customerCookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"rating\":5,\"content\":\"服务很好\"}")).andExpect(jsonPath("$.code").value(0));
        var reviewId = service.list(sender, CUSTOMER, id).reviews().get(0).review().getId();
        var courierCookie = login(courier, COURIER);
        mvc.perform(get("/api/order/{id}/reviews", id).cookie(courierCookie))
            .andExpect(jsonPath("$.data.reviews[0].canAppeal").value(true))
            .andExpect(jsonPath("$.data.reviews[0].username").value("评价测试"))
            .andExpect(jsonPath("$.data.reviews[0].review.id").value(reviewId.toString()));
        mvc.perform(post("/api/review/{id}/appeals", reviewId).cookie(courierCookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"请核实\"}")).andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/review/appeals/admin").cookie(customerCookie))
            .andExpect(jsonPath("$.code").value(ResultCodeEnum.NO_PERMISSION.getCode()));
        var appealId = service.list(courier, COURIER, id).reviews().get(0).appeal().getId();
        var adminCookie = login(admin, ADMIN);
        mvc.perform(post("/api/review/appeals/{id}/resolve", appealId).cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"UPHELD\",\"reason\":\"申诉成立\"}")).andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/review/appeals/admin").param("orderId", id.toString()).cookie(adminCookie))
            .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].status").value("UPHELD"));
        mvc.perform(get("/api/order/{id}/reviews", id).cookie(customerCookie))
            .andExpect(jsonPath("$.data.reviews[0].review.status").value("VOID"))
            .andExpect(jsonPath("$.data.reviews[0].review.content").value("服务很好"));
    }
}
