package cn.njust.campusexpress.model.review.vo;

import lombok.Data;

import java.util.List;

//订单评价列表VO
@Data
public class OrderReviewsVO {
    private List<ReviewVO> reviews;
    private boolean canReview;
}
