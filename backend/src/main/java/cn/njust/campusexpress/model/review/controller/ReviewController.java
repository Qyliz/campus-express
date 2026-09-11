package cn.njust.campusexpress.model.review.controller;
import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.*;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.model.review.dto.*;
import cn.njust.campusexpress.model.review.entity.ReviewAppeal;
import cn.njust.campusexpress.model.review.vo.OrderReviewsVO;
import cn.njust.campusexpress.model.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;
    private Long userId() { return StpUtil.getLoginIdAsLong(); }
    private UserRoleEnum role() { return UserRoleEnum.valueOf((String) StpUtil.getTokenSession().get("role")); }
    @GetMapping("/order/{id}/reviews")
    public Result<OrderReviewsVO> list(@PathVariable Long id) {
        return Result.success(service.list(userId(), role(), id));
    }
    @PostMapping("/order/{id}/reviews")
    public Result<Long> create(@PathVariable Long id, @Valid @RequestBody ReviewDTO dto) {
        return Result.success(service.create(userId(), role(), id, dto));
    }
    @PostMapping("/review/{id}/appeals")
    public Result<Long> appeal(@PathVariable Long id, @Valid @RequestBody AppealDTO dto) {
        return Result.success(service.appeal(userId(), role(), id, dto));
    }
    @GetMapping("/review/appeals/admin")
    public Result<PageResult<ReviewAppeal>> adminList(@Valid AppealQueryDTO dto) {
        return Result.success(service.adminList(userId(), role(), dto));
    }
    @PostMapping("/review/appeals/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id, @Valid @RequestBody ResolveAppealDTO dto) {
        service.resolve(userId(), role(), id, dto);
        return Result.success();
    }
}
