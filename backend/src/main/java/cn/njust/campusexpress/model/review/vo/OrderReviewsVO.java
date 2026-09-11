package cn.njust.campusexpress.model.review.vo;
import java.util.List;
public record OrderReviewsVO(List<ReviewVO> reviews, boolean canReview) {}
