package cn.njust.campusexpress.model.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
public class DeliveryException {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Long courierId;
    private String type;
    private String description;
    private Integer status;
    private Long adminId;
    private String resolution;
    private String resolutionDescription;
    private Date createTime;
    private Date resolvedTime;
}
