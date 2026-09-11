package cn.njust.campusexpress.model.review.service.impl;

import cn.njust.campusexpress.model.review.service.ReviewService;

import cn.njust.campusexpress.common.*;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.review.dto.*;
import cn.njust.campusexpress.model.review.entity.*;
import cn.njust.campusexpress.model.review.mapper.*;
import cn.njust.campusexpress.model.review.vo.*;
import cn.njust.campusexpress.model.user.entity.RoleAccount;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.mapper.UserMapper;
import cn.njust.campusexpress.model.user.service.RoleAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final OrderService orders;
    private final RoleAccountService accounts;
    private final ServiceReviewMapper reviews;
    private final ReviewAppealMapper appeals;
    private final UserMapper users;

    private BusinessException invalid(String message) {
        return new BusinessException(ResultCodeEnum.PARAM_ERROR, message);
    }

    private RoleAccount requireRole(Long userId, UserRoleEnum role, UserRoleEnum expected) {
        if (role != expected) throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
        var account = accounts.getByUserAndRole(userId, role);
        if (account == null || account.getStatus() != UserStatusEnum.NORMAL)
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION, "当前角色账户不可用");
        return account;
    }

    private String text(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 500)
            throw invalid("请填写1～500字的说明");
        return value.strip();
    }

    private ServiceReview getReview(Long id) {
        var review = reviews.selectById(id);
        if (review == null) throw invalid("评价不存在");
        return review;
    }

    /** 评价人 userId 就是 user 表主键（只有收寄件人能评价），一次批量查询取用户名，不逐条查。 */
    private Map<Long, String> namesOf(List<ServiceReview> items) {
        if (items.isEmpty()) return Map.of();
        return users.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, items.stream().map(ServiceReview::getUserId).collect(Collectors.toSet())))
                .stream().collect(Collectors.toMap(User::getId, User::getUsername));
    }

    @Override
    public OrderReviewsVO list(Long userId, UserRoleEnum role, Long orderId) {
        // 复用订单详情的完整参与者校验，包括收件手机号匹配规则。
        var order = orders.detail(userId, role, orderId).order();
        var items = reviews.selectList(new LambdaQueryWrapper<ServiceReview>()
            .eq(ServiceReview::getOrderId, orderId)
            .orderByAsc(ServiceReview::getCreateTime).orderByAsc(ServiceReview::getId));
        var history = appeals.selectList(new LambdaQueryWrapper<ReviewAppeal>()
            .eq(ReviewAppeal::getOrderId, orderId)).stream()
            .collect(Collectors.toMap(ReviewAppeal::getReviewId, a -> a));
        var names = namesOf(items);
        return new OrderReviewsVO(items.stream().map(r -> new ReviewVO(r, history.get(r.getId()),
            role == UserRoleEnum.COURIER && r.getStatus() == ReviewStatusEnum.VALID && !history.containsKey(r.getId()),
            names.get(r.getUserId()))).toList(),
            role == UserRoleEnum.CUSTOMER && order.getOrderStatus() == OrderStatusEnum.COMPLETED && order.getCourierId() != null
                && items.stream().noneMatch(r -> Objects.equals(r.getUserId(), userId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, UserRoleEnum role, Long orderId, ReviewDTO dto) {
        requireRole(userId, role, UserRoleEnum.CUSTOMER);
        var order = orders.detail(userId, role, orderId).order();
        if (order.getOrderStatus() != OrderStatusEnum.COMPLETED || order.getCourierId() == null) throw invalid("仅已完成的配送订单可以评价");
        if (dto.rating() == null || dto.rating() < 1 || dto.rating() > 5) throw invalid("评分必须为1～5星");
        var review = new ServiceReview();
        review.setOrderId(orderId);
        review.setUserId(userId);
        review.setCourierId(order.getCourierId());
        review.setRating(dto.rating());
        review.setContent(text(dto.content()));
        review.setStatus(ReviewStatusEnum.VALID);
        review.setCreateTime(new Date());
        try {
            if (reviews.insert(review) != 1) throw invalid("保存评价失败");
        } catch (DuplicateKeyException e) {
            throw invalid("您已评价过该订单");
        }
        return review.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long appeal(Long userId, UserRoleEnum role, Long reviewId, AppealDTO dto) {
        var courier = requireRole(userId, role, UserRoleEnum.COURIER);
        var review = getReview(reviewId);
        orders.detail(userId, role, review.getOrderId());
        if (!Objects.equals(courier.getId(), review.getCourierId()))
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
        if (review.getStatus() != ReviewStatusEnum.VALID) throw invalid("评价已作废");
        var appeal = new ReviewAppeal();
        appeal.setReviewId(reviewId);
        appeal.setOrderId(review.getOrderId());
        appeal.setCourierId(courier.getId());
        appeal.setReason(text(dto.reason()));
        appeal.setStatus(AppealStatusEnum.PENDING);
        appeal.setCreateTime(new Date());
        try {
            if (appeals.insert(appeal) != 1) throw invalid("保存申诉失败");
        } catch (DuplicateKeyException e) {
            throw invalid("该评价已提交过申诉");
        }
        return appeal.getId();
    }

    @Override
    public PageResult<ReviewAppeal> adminList(Long userId, UserRoleEnum role, AppealQueryDTO dto) {
        requireRole(userId, role, UserRoleEnum.ADMIN);
        return PageResult.of(appeals.selectPage(new Page<>(dto.getCurrentPage(), 10),
            new LambdaQueryWrapper<ReviewAppeal>()
                .eq(dto.getStatus() != null, ReviewAppeal::getStatus, dto.getStatus())
                .eq(dto.getOrderId() != null, ReviewAppeal::getOrderId, dto.getOrderId())
                .orderByDesc(ReviewAppeal::getCreateTime).orderByDesc(ReviewAppeal::getId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long userId, UserRoleEnum role, Long id, ResolveAppealDTO dto) {
        var admin = requireRole(userId, role, UserRoleEnum.ADMIN);
        var appeal = appeals.selectById(id);
        if (appeal == null) throw invalid("申诉不存在");
        if (appeal.getStatus() != AppealStatusEnum.PENDING) throw invalid("申诉已处理，请刷新");
        // 原来由 @Min(1) 挡住「把申诉改回待处理」，status 换成枚举后 PENDING 也是合法值，这条规则只能留在这里。
        if (dto.status() == AppealStatusEnum.PENDING) throw invalid("处理结果无效");
        appeal.setResolutionReason(text(dto.reason()));
        appeal.setStatus(dto.status());
        appeal.setAdminId(admin.getId());
        appeal.setResolvedTime(new Date());
        if (dto.status() == AppealStatusEnum.UPHELD) {
            var review = getReview(appeal.getReviewId());
            review.setStatus(ReviewStatusEnum.VOID);
            if (reviews.updateById(review) != 1) throw invalid("评价作废失败");
        }
        if (appeals.updateById(appeal) != 1) throw invalid("保存处理结果失败");
    }
}
