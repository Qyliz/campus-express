package cn.njust.campusexpress.model.order.controller;
import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.*;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.entity.ExpressOrder;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.order.vo.OrderDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;
    @PostMapping("/{id}/exceptions")
    public Result<Long> reportException(@PathVariable Long id, @Valid @RequestBody ReportExceptionDTO dto) {
        return Result.success(service.reportException(userId(), role(), id, dto));
    }
    private Long userId() { return StpUtil.getLoginIdAsLong(); }
    private UserRoleEnum role() { return UserRoleEnum.valueOf((String) StpUtil.getTokenSession().get("role")); }
    // 收寄件人发布订单，初始状态为待支付。
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(service.create(userId(), role(), dto));
    }
    // 查询订单详情及配送进度，具体访问权限由服务层校验。
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(service.detail(userId(), role(), id));
    }
    // 查询当前收寄件人发布的订单。
    @GetMapping("/mine")
    public Result<PageResult<ExpressOrder>> mine(@Valid OrderQueryDTO dto) {
        return Result.success(service.list(userId(), role(), "mine", dto));
    }
    // 查询接单大厅中的待接单订单。
    @GetMapping("/available")
    public Result<PageResult<ExpressOrder>> available(@Valid OrderQueryDTO dto) {
        return Result.success(service.list(userId(), role(), "available", dto));
    }
    // 查询当前配送员已经接下的任务。
    @GetMapping("/assigned")
    public Result<PageResult<ExpressOrder>> assigned(@Valid OrderQueryDTO dto) {
        return Result.success(service.list(userId(), role(), "assigned", dto));
    }
    // 管理员按状态和订单编号查询订单。
    @GetMapping("/admin")
    public Result<PageResult<ExpressOrder>> admin(@Valid OrderQueryDTO dto) {
        return Result.success(service.list(userId(), role(), "admin", dto));
    }
    // 模拟支付成功后，将订单开放给配送员接单。
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        service.act(userId(), role(), id, "pay", null);
        return Result.success();
    }
    // 配送员接单，订单进入待揽收状态。
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        service.act(userId(), role(), id, "accept", null);
        return Result.success();
    }
    // 配送员确认揽收，开始配送。
    @PostMapping("/{id}/pickup")
    public Result<Void> pickup(@PathVariable Long id) {
        service.act(userId(), role(), id, "pickup", null);
        return Result.success();
    }
    // 配送员确认送达，等待收寄件人取件。
    @PostMapping("/{id}/deliver")
    public Result<Void> deliver(@PathVariable Long id) {
        service.act(userId(), role(), id, "deliver", null);
        return Result.success();
    }
    // 收寄件人确认取件，完成订单。
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        service.act(userId(), role(), id, "complete", null);
        return Result.success();
    }
    // 收寄件人在接单前取消订单，已支付的订单模拟退款。
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody CancelOrderDTO dto) {
        service.act(userId(), role(), id, "cancel", dto.getReason());
        return Result.success();
    }
    // 管理员填写原因后取消未完成订单。
    @PostMapping("/{id}/admin-cancel")
    public Result<Void> admincancel(@PathVariable Long id, @Valid @RequestBody CancelOrderDTO dto) {
        service.act(userId(), role(), id, "admin-cancel", dto.getReason());
        return Result.success();
    }
}
