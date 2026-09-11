package cn.njust.campusexpress.model.review.vo;
import cn.njust.campusexpress.model.review.entity.*;
/** 评价条目。username 由 user 表解析，供用户端展示；管理端申诉页仍按裸 ID 定位账户。 */
public record ReviewVO(ServiceReview review, ReviewAppeal appeal, boolean canAppeal, String username) {}
