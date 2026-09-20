package cn.njust.campusexpress.model.review.entity;

import cn.njust.campusexpress.common.enums.ReviewStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class ServiceReview {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long courierId;
    private Integer rating;
    private String content;
    private ReviewStatusEnum status;
    private Date createTime;
}
