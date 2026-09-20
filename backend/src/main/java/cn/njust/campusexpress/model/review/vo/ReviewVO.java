package cn.njust.campusexpress.model.review.vo;

import cn.njust.campusexpress.model.review.entity.ReviewAppeal;
import cn.njust.campusexpress.model.review.entity.ServiceReview;
import lombok.Data;

//订单评价VO
@Data
public class ReviewVO {
    private ServiceReview review;
    private ReviewAppeal appeal;
    private boolean canAppeal;
    private String username;
}
