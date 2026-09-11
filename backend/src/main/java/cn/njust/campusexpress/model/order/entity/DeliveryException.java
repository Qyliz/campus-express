package cn.njust.campusexpress.model.order.entity;

import cn.njust.campusexpress.common.enums.ExceptionResolutionEnum;
import cn.njust.campusexpress.common.enums.ExceptionStatusEnum;
import cn.njust.campusexpress.common.enums.ExceptionTypeEnum;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
public class DeliveryException {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Long courierId;
    private ExceptionTypeEnum type;
    private String description;
    private ExceptionStatusEnum status;
    private Long adminId;
    private ExceptionResolutionEnum resolution;
    private String resolutionDescription;
    private Date createTime;
    private Date resolvedTime;
}
