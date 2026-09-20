package cn.njust.campusexpress.model.order.entity;

import cn.njust.campusexpress.common.enums.OrderStatusEnum;
import cn.njust.campusexpress.common.enums.PaymentStatusEnum;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExpressOrder {
    private Long id;
    private Long customerId;
    private Long courierId;
    private String pickupAddress;
    private String pickupName;
    private String pickupPhone;
    private String deliveryAddress;
    private String deliveryName;
    private String deliveryPhone;

    @TableField(exist = false)
    private boolean pendingException;

    @TableField(exist = false)
    private boolean createdByMe;

    @TableField(exist = false)
    private boolean receivedByMe;

    @TableField(exist = false)
    private String courierName;

    @TableField(exist = false)
    private String courierPhone;

    private String itemDescription;
    private String remark;
    private BigDecimal fee;
    private OrderStatusEnum orderStatus;
    private PaymentStatusEnum paymentStatus;

    @Version
    private Integer version;

    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
}
