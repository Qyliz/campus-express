package cn.njust.campusexpress.model.order.entity;
import com.baomidou.mybatisplus.annotation.*;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.OrderStatusEnum;
import lombok.Data;
import java.util.Date;
@Data
public class OrderStatusRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private OrderStatusEnum fromStatus;
    private OrderStatusEnum toStatus;
    private Long operatorId;
    private UserRoleEnum operatorRole;
    private String description;
    private Date createTime;
}
