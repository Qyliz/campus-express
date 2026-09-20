package cn.njust.campusexpress.model.review.controller;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.util.SessionUtil;
import cn.njust.campusexpress.model.review.dto.AppealDTO;
import cn.njust.campusexpress.model.review.dto.AppealQueryDTO;
import cn.njust.campusexpress.model.review.dto.ResolveAppealDTO;
import cn.njust.campusexpress.model.review.dto.ReviewDTO;
import cn.njust.campusexpress.model.review.entity.ReviewAppeal;
import cn.njust.campusexpress.model.review.service.ReviewService;
import cn.njust.campusexpress.model.review.vo.OrderReviewsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

//评价反馈控制器
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    //查询订单的评价
    @GetMapping("/order/{id}/reviews")
    public Result<OrderReviewsVO> list(@PathVariable Long id) {
        return Result.success(service.list(SessionUtil.userId(), SessionUtil.role(), id));
    }

    //发表订单评价
    @PostMapping("/order/{id}/reviews")
    public Result<Long> create(@PathVariable Long id, @Valid @RequestBody ReviewDTO dto) {
        return Result.success(service.create(SessionUtil.userId(), SessionUtil.role(), id, dto));
    }

    //申诉评价
    @PostMapping("/review/{id}/appeals")
    public Result<Long> appeal(@PathVariable Long id, @Valid @RequestBody AppealDTO dto) {
        return Result.success(service.appeal(SessionUtil.userId(), SessionUtil.role(), id, dto));
    }

    //查询申诉列表
    @GetMapping("/review/appeals/admin")
    public Result<PageResult<ReviewAppeal>> adminList(@Valid AppealQueryDTO dto) {
        return Result.success(service.adminList(SessionUtil.userId(), SessionUtil.role(), dto));
    }

    //处理申诉
    @PostMapping("/review/appeals/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id, @Valid @RequestBody ResolveAppealDTO dto) {
        service.resolve(SessionUtil.userId(), SessionUtil.role(), id, dto);
        return Result.success();
    }
}
