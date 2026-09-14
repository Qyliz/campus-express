package cn.njust.campusexpress.order;

import cn.njust.campusexpress.IntegrationTestSupport;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.entity.*;
import cn.njust.campusexpress.model.order.mapper.*;
import cn.njust.campusexpress.model.order.service.*;
import cn.njust.campusexpress.model.user.entity.*;
import cn.njust.campusexpress.model.user.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static cn.njust.campusexpress.common.enums.UserRoleEnum.*;
import static cn.njust.campusexpress.common.enums.OrderStatusEnum.*;
import static cn.njust.campusexpress.common.enums.OrderActionEnum.*;
import static cn.njust.campusexpress.common.enums.PaymentStatusEnum.PAID;
import static cn.njust.campusexpress.common.enums.PaymentStatusEnum.REFUNDED;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class OrderFlowTest extends IntegrationTestSupport {
    @Autowired OrderService service;
    @Autowired ExpressOrderMapper orders;
    @Autowired OrderStatusRecordMapper records;
    @Autowired PlatformTransactionManager transactionManager;

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
        Long customer = createUser(CUSTOMER, "订单测试"), courier = createUser(COURIER, "订单测试");
        Long id = service.create(customer, CUSTOMER, form());
        assertEquals(UNPAID, orders.selectById(id).getOrderStatus());
        service.act(customer, CUSTOMER, id, PAY, null);
        service.act(courier, COURIER, id, ACCEPT, null);
        service.act(courier, COURIER, id, PICKUP, null);
        service.act(courier, COURIER, id, DELIVER, null);
        service.act(customer, CUSTOMER, id, COMPLETE, null);
        ExpressOrder result = service.detail(customer, CUSTOMER, id).order();
        assertEquals(COMPLETED, result.getOrderStatus());
        assertEquals(PAID, result.getPaymentStatus());
        assertEquals(5, result.getVersion());
        assertEquals("13800000002", result.getDeliveryPhone());
        assertEquals(6, count(id));
        assertThrows(BusinessException.class, () -> service.act(customer, CUSTOMER, id, COMPLETE, null));
        assertEquals(6, count(id));
    }

    @Test
    void cancelUnpaidAndRefundPaidOrder() {
        Long customer = createUser(CUSTOMER, "订单测试");
        Long unpaid = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, unpaid, CANCEL, "暂时不需要");
        assertEquals(PaymentStatusEnum.UNPAID, orders.selectById(unpaid).getPaymentStatus());
        Long paid = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, paid, PAY, null);
        service.act(customer, CUSTOMER, paid, CANCEL, "地址填写错误");
        assertEquals(REFUNDED, orders.selectById(paid).getPaymentStatus());
        assertEquals(CANCELLED, orders.selectById(paid).getOrderStatus());
        assertEquals("地址填写错误", service.detail(customer, CUSTOMER, paid).records().get(2).getDescription());
    }

    @Test
    void adminCanCancelAcceptedOrderButCannotDeliver() {
        Long customer = createUser(CUSTOMER, "订单测试"), courier = createUser(COURIER, "订单测试"), admin = createUser(ADMIN, "订单测试");
        Long id = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, id, PAY, null);
        service.act(courier, COURIER, id, ACCEPT, null);
        assertThrows(BusinessException.class, () -> service.act(admin, ADMIN, id, PICKUP, null));
        assertThrows(BusinessException.class, () -> service.act(customer, CUSTOMER, id, CANCEL, "取消"));
        service.act(admin, ADMIN, id, ADMIN_CANCEL, "配送员无法继续配送");
        assertEquals(REFUNDED, orders.selectById(id).getPaymentStatus());
    }

    @Test
    void hallHidesContactsAndOtherUsersCannotReadOrChangeOrder() {
        Long customer = createUser(CUSTOMER, "订单测试"), other = createUser(CUSTOMER, "订单测试"), courier = createUser(COURIER, "订单测试");
        Long id = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, id, PAY, null);
        ExpressOrder summary = service.list(courier, COURIER, OrderListScopeEnum.AVAILABLE, new OrderQueryDTO())
                .getRecords().stream().filter(o -> o.getId().equals(id)).findFirst().orElseThrow();
        assertNull(summary.getPickupPhone());
        assertNull(summary.getDeliveryPhone());
        assertNull(summary.getPickupName());
        assertNull(summary.getRemark());
        assertThrows(BusinessException.class, () -> service.detail(other, CUSTOMER, id));
        assertThrows(BusinessException.class, () -> service.detail(courier, COURIER, id));
        assertThrows(BusinessException.class, () -> service.act(other, CUSTOMER, id, PAY, null));
    }

    @Test
    void selfAndDisabledCourierCannotAcceptAndStateCannotBeSkipped() {
        Long customer = createUser(CUSTOMER, "订单测试"), courier = createUser(COURIER, "订单测试");
        accounts.createAccount(customer, COURIER, UserStatusEnum.NORMAL);
        Long id = service.create(customer, CUSTOMER, form());
        assertThrows(BusinessException.class, () -> service.act(courier, COURIER, id, ACCEPT, null));
        service.act(customer, CUSTOMER, id, PAY, null);
        assertThrows(BusinessException.class, () -> service.act(customer, COURIER, id, ACCEPT, null));
        RoleAccount account = accounts.getByUserAndRole(courier, COURIER);
        for (UserStatusEnum state : List.of(UserStatusEnum.DISABLED, UserStatusEnum.REVIEWING, UserStatusEnum.REJECTED)) {
            accounts.updateStatus(account, COURIER, state);
            assertThrows(BusinessException.class, () -> service.act(courier, COURIER, id, ACCEPT, null));
        }
        assertEquals(AVAILABLE, orders.selectById(id).getOrderStatus());
        assertEquals(2, count(id));
    }

    @Test
    void staleVersionCannotOverwriteAcceptedOrder() {
        Long customer = createUser(CUSTOMER, "订单测试"), courier = createUser(COURIER, "订单测试");
        Long id = service.create(customer, CUSTOMER, form());
        service.act(customer, CUSTOMER, id, PAY, null);
        // MyBatis 一级缓存会复用实体对象，复制快照才能模拟另一个事务读到的旧版本。
        ExpressOrder stale = new ExpressOrder();
        org.springframework.beans.BeanUtils.copyProperties(orders.selectById(id), stale);
        service.act(courier, COURIER, id, ACCEPT, null);
        stale.setOrderStatus(CANCELLED);
        stale.setPaymentStatus(REFUNDED);
        assertEquals(0, orders.updateById(stale));
        assertEquals(AWAITING_PICKUP, orders.selectById(id).getOrderStatus());
        assertEquals(PAID, orders.selectById(id).getPaymentStatus());
        assertEquals(3, count(id));
    }

    // 匿名请求一律 401，与后面的登录态用例拆开，避免一个方法混多个场景。
    @Test
    void orderEndpointsRejectAnonymousRequests() throws Exception {
        mvc.perform(get("/api/order/mine")).andExpect(status().isUnauthorized());
    }

    // 下单与分页参数的校验全部走 @Valid，非法值统一返回 PARAM_ERROR。
    @Test
    void createOrderValidatesFeeAndPageParameters() throws Exception {
        Cookie cookie = registerAndLoginCustomer();
        mvc.perform(post("/api/order").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.code").value(1));
        String body = """
                {"pickupAddress":"站点","pickupName":"甲","pickupPhone":"13800000001",
                 "deliveryAddress":"宿舍","deliveryName":"乙","deliveryPhone":"13800000002",
                 "itemDescription":"书","fee":5.50}
                """;
        for (String fee : List.of("0", "-1", "10000", "1.001")) {
            mvc.perform(post("/api/order").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                    .content(body.replace("5.50", fee))).andExpect(jsonPath("$.code").value(1));
        }
        mvc.perform(get("/api/order/mine").cookie(cookie).param("currentPage", "0"))
                .andExpect(jsonPath("$.code").value(1));
    }

    // 雪花 id 超过 JS 的安全整数范围，HTTP 层必须序列化成字符串。
    @Test
    void orderIdIsSerializedAsString() throws Exception {
        Cookie cookie = registerAndLoginCustomer();
        String body = """
                {"pickupAddress":"站点","pickupName":"甲","pickupPhone":"13800000001",
                 "deliveryAddress":"宿舍","deliveryName":"乙","deliveryPhone":"13800000002",
                 "itemDescription":"书","fee":5.50}
                """;
        mvc.perform(post("/api/order").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").isString());
    }

    // 接单大厅是配送员专属，收寄件人访问返回 NO_PERMISSION。
    @Test
    void customerCannotEnterCourierHall() throws Exception {
        Cookie cookie = registerAndLoginCustomer();
        mvc.perform(get("/api/order/available").cookie(cookie)).andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousAcceptHasOneWinnerAndOneRecord() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<Long> couriers = new ArrayList<>();
        Long id = null;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            id = tx.execute(status -> {
                Long customer = createUser(CUSTOMER, "订单测试");
                couriers.add(createUser(COURIER, "订单测试"));
                couriers.add(createUser(COURIER, "订单测试"));
                Long orderId = service.create(customer, CUSTOMER, form());
                createdOrderIds.add(orderId);
                service.act(customer, CUSTOMER, orderId, PAY, null);
                return orderId;
            });
            Long orderId = id;
            CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Long courier : couriers) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("接单启动超时");
                    try { service.act(courier, COURIER, orderId, ACCEPT, null); return true; }
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
            cleanupCreatedData();
        }
    }
}
