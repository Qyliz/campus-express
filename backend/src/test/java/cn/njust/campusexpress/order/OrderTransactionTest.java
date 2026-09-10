package cn.njust.campusexpress.order;

import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.model.order.dto.CreateOrderDTO;
import cn.njust.campusexpress.model.order.entity.OrderStatusRecord;
import cn.njust.campusexpress.model.order.mapper.*;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderTransactionTest {
    @Autowired OrderService service;
    @Autowired ExpressOrderMapper orders;
    @MockitoSpyBean OrderStatusRecordMapper records;
    @Autowired UserService users;
    @Autowired RoleAccountService accounts;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void recordFailureRollsBackPaymentAndVersion() {
        Long[] ids = new Long[2];
        try {
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                User user = new User();
                user.setUsername("订单事务测试");
                user.setGender(UserGenderEnum.UNKNOWN);
                user.setPassword("unused");
                users.save(user);
                ids[0] = user.getId();
                accounts.createAccount(user.getId(), UserRoleEnum.CUSTOMER, UserStatusEnum.NORMAL);
                CreateOrderDTO form = new CreateOrderDTO();
                form.setPickupAddress("站点");
                form.setPickupName("甲");
                form.setPickupPhone("13800000001");
                form.setDeliveryAddress("宿舍");
                form.setDeliveryName("乙");
                form.setDeliveryPhone("13800000002");
                form.setItemDescription("书籍");
                form.setFee(new BigDecimal("5.00"));
                ids[1] = service.create(user.getId(), UserRoleEnum.CUSTOMER, form);
            });
            doThrow(new IllegalStateException("模拟记录保存失败")).when(records).insert(any(OrderStatusRecord.class));
            assertThrows(IllegalStateException.class,
                    () -> service.act(ids[0], UserRoleEnum.CUSTOMER, ids[1], "pay", null));
            var order = orders.selectById(ids[1]);
            assertEquals(0, order.getOrderStatus());
            assertEquals(0, order.getPaymentStatus());
            assertEquals(0, order.getVersion());
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from order_status_record where order_id = ?", Integer.class, ids[1]));
        } finally {
            reset(records);
            if (ids[1] != null) {
                jdbc.update("delete from order_status_record where order_id = ?", ids[1]);
                jdbc.update("delete from express_order where id = ?", ids[1]);
            }
            if (ids[0] != null) {
                jdbc.update("delete from customer where user_id = ?", ids[0]);
                jdbc.update("delete from user where id = ?", ids[0]);
            }
        }
    }
}
