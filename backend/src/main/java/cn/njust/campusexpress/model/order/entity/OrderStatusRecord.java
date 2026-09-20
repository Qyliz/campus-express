package cn.njust.campusexpress.model.order.entity;

import cn.njust.campusexpress.common.enums.OrderStatusEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import lombok.Data;

import java.util.Date;

@Data
public class OrderStatusRecord {
    private Long id;
    private Long orderId;
    private OrderStatusEnum fromStatus;
    private OrderStatusEnum toStatus;
    private Long operatorId;
    private UserRoleEnum operatorRole;
    private String description;
    private Date createTime;
}
