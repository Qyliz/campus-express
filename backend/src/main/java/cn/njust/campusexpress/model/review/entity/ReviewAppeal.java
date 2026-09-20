package cn.njust.campusexpress.model.review.entity;

import cn.njust.campusexpress.common.enums.AppealStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class ReviewAppeal {
    private Long id;
    private Long reviewId;
    private Long orderId;
    private Long courierId;
    private String reason;
    private AppealStatusEnum status;
    private Long adminId;
    private String resolutionReason;
    private Date createTime;
    private Date resolvedTime;
}
