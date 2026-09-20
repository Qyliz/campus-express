package cn.njust.campusexpress.order;

import cn.njust.campusexpress.IntegrationTestSupport;
import cn.njust.campusexpress.common.enums.OrderActionEnum;
import cn.njust.campusexpress.common.enums.OrderStatusEnum;
import cn.njust.campusexpress.common.enums.PaymentStatusEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.model.order.dto.CreateOrderDTO;
import cn.njust.campusexpress.model.order.entity.OrderStatusRecord;
import cn.njust.campusexpress.model.order.mapper.ExpressOrderMapper;
import cn.njust.campusexpress.model.order.mapper.OrderStatusRecordMapper;
import cn.njust.campusexpress.model.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

//验证订单状态变更与流转记录写入保持事务一致性
class OrderTransactionTest extends IntegrationTestSupport {
    @Autowired
    OrderService service;
    @Autowired
    ExpressOrderMapper orders;
    @MockitoSpyBean
    OrderStatusRecordMapper records;
    @Autowired
    PlatformTransactionManager manager;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void recordFailureRollsBackPaymentAndVersion() {
        Long[] ids = new Long[2];
        try {
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                ids[0] = createUser(UserRoleEnum.CUSTOMER, "订单事务测试");
                CreateOrderDTO form = new CreateOrderDTO();
                form.setPickupAddress("站点");
                form.setPickupName("甲");
                form.setPickupPhone("13800000001");
                form.setDeliveryAddress("宿舍");
                form.setDeliveryName("乙");
                form.setDeliveryPhone("13800000002");
                form.setItemDescription("书籍");
                form.setFee(new BigDecimal("5.00"));
                ids[1] = service.create(ids[0], UserRoleEnum.CUSTOMER, form);
                createdOrderIds.add(ids[1]);
            });
            doThrow(new IllegalStateException("模拟记录保存失败")).when(records).insert(any(OrderStatusRecord.class));
            assertThrows(IllegalStateException.class,
                    () -> service.act(ids[0], UserRoleEnum.CUSTOMER, ids[1], OrderActionEnum.PAY, null));
            var order = orders.selectById(ids[1]);
            assertEquals(OrderStatusEnum.UNPAID, order.getOrderStatus());
            assertEquals(PaymentStatusEnum.UNPAID, order.getPaymentStatus());
            assertEquals(0, order.getVersion());
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from order_status_record where order_id = ?", Integer.class, ids[1]));
        } finally {
            reset(records);
            cleanupCreatedData();
        }
    }
}
