package cn.njust.campusexpress.model.order.entity;
import com.baomidou.mybatisplus.annotation.*;
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
    private String deliveryEmail;
    @TableField(exist = false)
    private boolean pendingException;
    @TableField(exist = false)
    private boolean createdByMe;
    @TableField(exist = false)
    private boolean receivedByMe;
    private String itemDescription;
    private String remark;
    private BigDecimal fee;
    private Integer orderStatus;
    private Integer paymentStatus;
    @Version
    private Integer version;
    private Date createTime;
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
}
