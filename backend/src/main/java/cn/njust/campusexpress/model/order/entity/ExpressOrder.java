package cn.njust.campusexpress.model.order.entity;
import com.baomidou.mybatisplus.annotation.*;
import cn.njust.campusexpress.common.enums.OrderStatusEnum;
import cn.njust.campusexpress.common.enums.PaymentStatusEnum;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
@Data
public class ExpressOrder {
    @TableId(type = IdType.ASSIGN_ID)
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
    /** 接单骑手的展示信息。courierId 是 courier 表主键，姓名和手机号在 user 表，由 attachCouriers 批量回填。 */
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
