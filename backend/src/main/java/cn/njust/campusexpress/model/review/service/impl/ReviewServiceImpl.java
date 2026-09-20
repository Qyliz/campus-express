package cn.njust.campusexpress.model.review.service.impl;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.review.dto.AppealDTO;
import cn.njust.campusexpress.model.review.dto.AppealQueryDTO;
import cn.njust.campusexpress.model.review.dto.ResolveAppealDTO;
import cn.njust.campusexpress.model.review.dto.ReviewDTO;
import cn.njust.campusexpress.model.review.entity.ReviewAppeal;
import cn.njust.campusexpress.model.review.entity.ServiceReview;
import cn.njust.campusexpress.model.review.mapper.ReviewAppealMapper;
import cn.njust.campusexpress.model.review.mapper.ServiceReviewMapper;
import cn.njust.campusexpress.model.review.service.ReviewService;
import cn.njust.campusexpress.model.review.vo.OrderReviewsVO;
import cn.njust.campusexpress.model.review.vo.ReviewVO;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.mapper.UserMapper;
import cn.njust.campusexpress.model.user.service.AccountGuard;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.njust.campusexpress.common.exception.BusinessException.invalid;

//评价申诉模块ServiceImpl
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final OrderService orders;
    private final AccountGuard guard;
    private final ServiceReviewMapper reviews;
    private final ReviewAppealMapper appeals;
    private final UserMapper users;

    //查询订单评价、申诉记录及当前用户可执行的操作
    @Override
    public OrderReviewsVO list(Long userId, UserRoleEnum role, Long orderId) {
        //复用订单详情的完整参与者校验，包括收件手机号匹配规则
        var order = orders.detail(userId, role, orderId).getOrder();
        var items = reviews.selectList(new LambdaQueryWrapper<ServiceReview>()
                .eq(ServiceReview::getOrderId, orderId)
                .orderByAsc(ServiceReview::getCreateTime).orderByAsc(ServiceReview::getId));
        //一次查询并按评价编号索引申诉，避免逐条读取申诉记录
        var history = appeals.selectList(new LambdaQueryWrapper<ReviewAppeal>()
                        .eq(ReviewAppeal::getOrderId, orderId)).stream()
                .collect(Collectors.toMap(ReviewAppeal::getReviewId, a -> a));
        var names = namesOf(items);
        //把评价、申诉和展示用户名合并，并按当前配送员计算每条评价能否申诉
        List<ReviewVO> reviewItems = items.stream().map(review -> {
            ReviewVO item = new ReviewVO();
            item.setReview(review);
            item.setAppeal(history.get(review.getId()));
            item.setCanAppeal(role == UserRoleEnum.COURIER && review.getStatus() == ReviewStatusEnum.VALID
                    && !history.containsKey(review.getId()));
            item.setUsername(names.get(review.getUserId()));
            return item;
        }).toList();
        OrderReviewsVO result = new OrderReviewsVO();
        result.setReviews(reviewItems);
        result.setCanReview(role == UserRoleEnum.CUSTOMER
                && order.getOrderStatus() == OrderStatusEnum.COMPLETED && order.getCourierId() != null
                && items.stream().noneMatch(review -> Objects.equals(review.getUserId(), userId)));
        return result;
    }

    //收寄件人发表评价，返回评价ID
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, UserRoleEnum role, Long orderId, ReviewDTO dto) {
        guard.requireRole(userId, role, UserRoleEnum.CUSTOMER);
        var order = orders.detail(userId, role, orderId).getOrder();
        if (order.getOrderStatus() != OrderStatusEnum.COMPLETED || order.getCourierId() == null)
            throw invalid("仅已完成的配送订单可以评价");
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5)
            throw invalid("评分必须为1～5星");
        var review = new ServiceReview();
        review.setOrderId(orderId);
        review.setUserId(userId);
        review.setCourierId(order.getCourierId());
        review.setRating(dto.getRating());
        review.setContent(text(dto.getContent()));
        review.setStatus(ReviewStatusEnum.VALID);
        //唯一索引兜底并发重复评价，将数据库异常转换为稳定业务提示
        try {
            if (reviews.insert(review) != 1) throw invalid("保存评价失败");
        } catch (DuplicateKeyException e) {
            throw invalid("您已评价过该订单");
        }
        return review.getId();
    }

    //配送员提交评价申诉，返回申诉ID
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long appeal(Long userId, UserRoleEnum role, Long reviewId, AppealDTO dto) {
        var courier = guard.requireRole(userId, role, UserRoleEnum.COURIER);
        var review = getReview(reviewId);
        orders.detail(userId, role, review.getOrderId());
        if (!Objects.equals(courier.getId(), review.getCourierId()))
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
        if (review.getStatus() != ReviewStatusEnum.VALID)
            throw invalid("评价已作废");
        var appeal = new ReviewAppeal();
        appeal.setReviewId(reviewId);
        appeal.setOrderId(review.getOrderId());
        appeal.setCourierId(courier.getId());
        appeal.setReason(text(dto.getReason()));
        appeal.setStatus(AppealStatusEnum.PENDING);
        //唯一索引兜底并发重复申诉，将数据库异常转换为稳定业务提示
        try {
            if (appeals.insert(appeal) != 1) throw invalid("保存申诉失败");
        } catch (DuplicateKeyException e) {
            throw invalid("该评价已提交过申诉");
        }
        return appeal.getId();
    }

    //管理员分页查询评价申诉
    @Override
    public PageResult<ReviewAppeal> adminList(Long userId, UserRoleEnum role, AppealQueryDTO dto) {
        guard.requireRole(userId, role, UserRoleEnum.ADMIN);
        return PageResult.of(appeals.selectPage(PageResult.pageOf(dto.getCurrentPage()),
                new LambdaQueryWrapper<ReviewAppeal>()
                        .eq(dto.getStatus() != null, ReviewAppeal::getStatus, dto.getStatus())
                        .eq(dto.getOrderId() != null, ReviewAppeal::getOrderId, dto.getOrderId())
                        .orderByDesc(ReviewAppeal::getCreateTime).orderByDesc(ReviewAppeal::getId)));
    }

    //管理员处理评价申诉
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long userId, UserRoleEnum role, Long id, ResolveAppealDTO dto) {
        var admin = guard.requireRole(userId, role, UserRoleEnum.ADMIN);
        var appeal = appeals.selectById(id);
        if (appeal == null) throw invalid("申诉不存在");
        if (appeal.getStatus() != AppealStatusEnum.PENDING)
            throw invalid("申诉已处理，请刷新");
        if (dto.getStatus() == AppealStatusEnum.PENDING)
            throw invalid("处理结果无效");
        appeal.setResolutionReason(text(dto.getReason()));
        appeal.setStatus(dto.getStatus());
        appeal.setAdminId(admin.getId());
        appeal.setResolvedTime(new Date());
        if (dto.getStatus() == AppealStatusEnum.UPHELD) {
            var review = getReview(appeal.getReviewId());
            review.setStatus(ReviewStatusEnum.VOID);
            if (reviews.updateById(review) != 1) throw invalid("评价作废失败");
        }
        if (appeals.updateById(appeal) != 1) throw invalid("保存处理结果失败");
    }

    //批量查询评价人的展示名称
    private Map<Long, String> namesOf(List<ServiceReview> items) {
        if (items.isEmpty()) return Map.of();
        return users.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, items.stream().map(ServiceReview::getUserId).collect(Collectors.toSet())))
                .stream().collect(Collectors.toMap(User::getId, User::getUsername));
    }

    //读取评价
    private ServiceReview getReview(Long id) {
        var review = reviews.selectById(id);
        if (review == null) throw invalid("评价不存在");
        return review;
    }

    //校验并清理评价或申诉内容
    private String text(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 500)
            throw invalid("请填写1～500字的说明");
        return value.strip();
    }
}
