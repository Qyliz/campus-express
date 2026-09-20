package cn.njust.campusexpress.model.review.service;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.model.review.dto.*;
import cn.njust.campusexpress.model.review.entity.ReviewAppeal;
import cn.njust.campusexpress.model.review.vo.OrderReviewsVO;

//评价与申诉模块Service
public interface ReviewService {
    //查询订单评价、申诉记录及当前用户可执行的操作
    OrderReviewsVO list(Long userId, UserRoleEnum role, Long orderId);

    //收寄件人发表评价，返回评价ID
    Long create(Long userId, UserRoleEnum role, Long orderId, ReviewDTO dto);

    //配送员提交评价申诉，返回申诉ID
    Long appeal(Long userId, UserRoleEnum role, Long reviewId, AppealDTO dto);

    //管理员分页查询评价申诉
    PageResult<ReviewAppeal> adminList(Long userId, UserRoleEnum role, AppealQueryDTO dto);

    //管理员处理评价申诉
    void resolve(Long userId, UserRoleEnum role, Long id, ResolveAppealDTO dto);
}
