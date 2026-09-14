package cn.njust.campusexpress.model.order.controller;
import cn.njust.campusexpress.common.*;
import cn.njust.campusexpress.common.util.SessionUtil;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.entity.DeliveryException;
import cn.njust.campusexpress.model.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exception")
@RequiredArgsConstructor
public class DeliveryExceptionController {
    private final OrderService service;
    @GetMapping("/admin")
    public Result<PageResult<DeliveryException>> list(@Valid ExceptionQueryDTO dto) {
        return Result.success(service.listExceptions(SessionUtil.userId(), SessionUtil.role(), dto));
    }
    @GetMapping("/{id}")
    public Result<DeliveryException> detail(@PathVariable Long id) {
        return Result.success(service.exceptionDetail(SessionUtil.userId(), SessionUtil.role(), id));
    }
    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id, @Valid @RequestBody ResolveExceptionDTO dto) {
        service.resolveException(SessionUtil.userId(), SessionUtil.role(), id, dto);
        return Result.success();
    }
}
