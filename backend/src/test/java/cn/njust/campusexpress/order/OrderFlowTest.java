package cn.njust.campusexpress.order;

import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.entity.*;
import cn.njust.campusexpress.model.order.mapper.*;
import cn.njust.campusexpress.model.order.service.*;
import cn.njust.campusexpress.model.user.entity.*;
import cn.njust.campusexpress.model.user.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static cn.njust.campusexpress.common.enums.UserRoleEnum.*;
import static cn.njust.campusexpress.model.order.service.OrderState.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderFlowTest {
    @Autowired private cn.njust.campusexpress.model.user.service.VerifyCodeService registrationCodes;
    @Autowired OrderService service;
    @Autowired ExpressOrderMapper orders;
    @Autowired OrderStatusRecordMapper records;
    @Autowired UserService users;
    @Autowired RoleAccountService accounts;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    private Long user(UserRoleEnum role) {
        User user = new User();
        user.setUsername("订单测试");
        user.setGender(UserGenderEnum.UNKNOWN);
        user.setPassword("unused");
        users.save(user);
        accounts.createAccount(user.getId(), role, UserStatusEnum.NORMAL);
        return user.getId();
    }

    private CreateOrderDTO form() {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setPickupAddress("快递站A区");
        dto.setPickupName("取件人");
        dto.setPickupPhone("13800000001");
        dto.setDeliveryAddress("宿舍3栋");
        dto.setDeliveryName("收件人");
        dto.setDeliveryPhone("13800000002");
        dto.setItemDescription("书籍一箱");
        dto.setRemark("请轻放");
        dto.setFee(new BigDecimal("5.50"));
        return dto;
    }

    private long count(Long id) {
        return records.selectCount(new LambdaQueryWrapper<OrderStatusRecord>().eq(OrderStatusRecord::getOrderId, id));
    }

    @Test
    void completeFlowAndSnapshots() {
        Long customer = user(CUSTOMER), courier = user(COURIER);
        Long id = service.create(customer, CUSTOMER, form());
        assertEquals(UNPAID, orders.selectById(id).getOrderStatus());
        service.act(customer, CUSTOMER, id, "pay", null);
        service.act(courier, COURIER, id, "accept", null);
        service.act(courier, COURIER, id, "pickup", null);
        service.act(courier, COURIER, id, "deliver", null);
        service.act(customer, CUSTOMER, id, "complete", null);
        ExpressOrder result = service.detail(customer, CUSTOMER, id).order();
        assertEquals(COMPLETED, result.getOrderStatus());
        assertEquals(PAID, result.getPaymentStatus());
        assertEquals(5, result.getVersion());
        assertEquals("13800000002", result.getDeliveryPhone());
        assertEquals(6, count(id));
        assertThrows(BusinessException.class, () -> service.act(customer, CUSTOMER, id, "complete", null));
        assertEquals(6, count(id));
    }

    @Test
    void cancelUnpaidAndRefundPaidOrder() {
        Long customer = user(CUSTOMER);
        Long unpaid = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, unpaid, "cancel", "暂时不需要");
        assertEquals(PAYMENT_UNPAID, orders.selectById(unpaid).getPaymentStatus());
        Long paid = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, paid, "pay", null);
        service.act(customer, CUSTOMER, paid, "cancel", "地址填写错误");
        assertEquals(REFUNDED, orders.selectById(paid).getPaymentStatus());
        assertEquals(CANCELLED, orders.selectById(paid).getOrderStatus());
        assertEquals("地址填写错误", service.detail(customer, CUSTOMER, paid).records().get(2).getDescription());
    }

    @Test
    void adminCanCancelAcceptedOrderButCannotDeliver() {
        Long customer = user(CUSTOMER), courier = user(COURIER), admin = user(ADMIN);
        Long id = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, id, "pay", null);
        service.act(courier, COURIER, id, "accept", null);
        assertThrows(BusinessException.class, () -> service.act(admin, ADMIN, id, "pickup", null));
        assertThrows(BusinessException.class, () -> service.act(customer, CUSTOMER, id, "cancel", "取消"));
        service.act(admin, ADMIN, id, "admin-cancel", "配送员无法继续配送");
        assertEquals(REFUNDED, orders.selectById(id).getPaymentStatus());
    }

    @Test
    void hallHidesContactsAndOtherUsersCannotReadOrChangeOrder() {
        Long customer = user(CUSTOMER), other = user(CUSTOMER), courier = user(COURIER);
        Long id = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, id, "pay", null);
        ExpressOrder summary = service.list(courier, COURIER, "available", new OrderQueryDTO())
                .getRecords().stream().filter(o -> o.getId().equals(id)).findFirst().orElseThrow();
        assertNull(summary.getPickupPhone());
        assertNull(summary.getDeliveryPhone());
        assertNull(summary.getPickupName());
        assertNull(summary.getRemark());
        assertThrows(BusinessException.class, () -> service.detail(other, CUSTOMER, id));
        assertThrows(BusinessException.class, () -> service.detail(courier, COURIER, id));
        assertThrows(BusinessException.class, () -> service.act(other, CUSTOMER, id, "pay", null));
    }

    @Test
    void selfAndDisabledCourierCannotAcceptAndStateCannotBeSkipped() {
        Long customer = user(CUSTOMER), courier = user(COURIER);
        accounts.createAccount(customer, COURIER, UserStatusEnum.NORMAL);
        Long id = service.create(customer, CUSTOMER, form());
        assertThrows(BusinessException.class, () -> service.act(courier, COURIER, id, "accept", null));
        service.act(customer, CUSTOMER, id, "pay", null);
        assertThrows(BusinessException.class, () -> service.act(customer, COURIER, id, "accept", null));
        RoleAccount account = accounts.getByUserAndRole(courier, COURIER);
        for (UserStatusEnum state : List.of(UserStatusEnum.DISABLED, UserStatusEnum.REVIEWING, UserStatusEnum.REJECTED)) {
            accounts.updateStatus(account, COURIER, state);
            assertThrows(BusinessException.class, () -> service.act(courier, COURIER, id, "accept", null));
        }
        assertEquals(AVAILABLE, orders.selectById(id).getOrderStatus());
        assertEquals(2, count(id));
    }

    @Test
    void staleVersionCannotOverwriteAcceptedOrder() {
        Long customer = user(CUSTOMER), courier = user(COURIER);
        Long id = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, id, "pay", null);
        // MyBatis 一级缓存会复用实体对象，复制快照才能模拟另一个事务读到的旧版本。
        ExpressOrder stale = new ExpressOrder();
        org.springframework.beans.BeanUtils.copyProperties(orders.selectById(id), stale);
        service.act(courier, COURIER, id, "accept", null);
        stale.setOrderStatus(CANCELLED);
        stale.setPaymentStatus(REFUNDED);
        assertEquals(0, orders.updateById(stale));
        assertEquals(AWAITING_PICKUP, orders.selectById(id).getOrderStatus());
        assertEquals(PAID, orders.selectById(id).getPaymentStatus());
        assertEquals(3, count(id));
    }

    @Test
    void httpAuthenticationValidationAndIdSerialization() throws Exception {
        mvc.perform(get("/api/order/mine")).andExpect(status().isUnauthorized());
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(cn.njust.campusexpress.user.RegistrationTestSupport.registration(registrationCodes).param("username", "订单测试")
                .param("password", "1234567").param("role", "CUSTOMER").param("gender", "UNKNOWN")
                .param("email", email)).andExpect(jsonPath("$.code").value(0));
        Cookie cookie = mvc.perform(post("/api/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"" + email + "\",\"password\":\"1234567\",\"role\":\"CUSTOMER\"}"))
                .andReturn().getResponse().getCookie("satoken");
        mvc.perform(post("/api/order").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.code").value(1));
        String body = """
                {"pickupAddress":"站点","pickupName":"甲","pickupPhone":"13800000001",
                 "deliveryAddress":"宿舍","deliveryName":"乙","deliveryPhone":"13800000002",
                 "itemDescription":"书","fee":5.50}
                """;
        mvc.perform(post("/api/order").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").isString());
        for (String fee : List.of("0", "-1", "10000", "1.001")) {
            mvc.perform(post("/api/order").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                    .content(body.replace("5.50", fee))).andExpect(jsonPath("$.code").value(1));
        }
        mvc.perform(get("/api/order/mine").cookie(cookie).param("currentPage", "0"))
                .andExpect(jsonPath("$.code").value(1));
        mvc.perform(get("/api/order/available").cookie(cookie)).andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousAcceptHasOneWinnerAndOneRecord() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<Long> createdUsers = new ArrayList<>();
        Long id = null;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            id = tx.execute(status -> {
                Long customer = user(CUSTOMER), first = user(COURIER), second = user(COURIER);
                createdUsers.addAll(List.of(customer, first, second));
                Long orderId = service.create(customer, CUSTOMER, form());
                service.act(customer, CUSTOMER, orderId, "pay", null);
                return orderId;
            });
            Long orderId = id;
            CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Long courier : createdUsers.subList(1, 3)) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("接单启动超时");
                    try { service.act(courier, COURIER, orderId, "accept", null); return true; }
                    catch (BusinessException expected) { return false; }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int winners = 0;
            for (Future<Boolean> future : futures) if (future.get(15, TimeUnit.SECONDS)) winners++;
            assertEquals(1, winners);
            assertEquals(AWAITING_PICKUP, orders.selectById(id).getOrderStatus());
            assertEquals(3, count(id));
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
            if (id != null) {
                jdbc.update("delete from order_status_record where order_id = ?", id);
                jdbc.update("delete from express_order where id = ?", id);
            }
            for (Long userId : createdUsers) {
                jdbc.update("delete from customer where user_id = ?", userId);
                jdbc.update("delete from courier where user_id = ?", userId);
                jdbc.update("delete from user where id = ?", userId);
            }
        }
    }
}
