package cn.njust.campusexpress.model.order.controller;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.util.SessionUtil;
import cn.njust.campusexpress.model.order.dto.ExceptionQueryDTO;
import cn.njust.campusexpress.model.order.dto.ResolveExceptionDTO;
import cn.njust.campusexpress.model.order.entity.DeliveryException;
import cn.njust.campusexpress.model.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

//配送异常控制器
@RestController
@RequestMapping("/api/exception")
@RequiredArgsConstructor
public class DeliveryExceptionController {
    private final OrderService service;

    //查询配送异常列表
    @GetMapping("/admin")
    public Result<PageResult<DeliveryException>> list(@Valid ExceptionQueryDTO dto) {
        return Result.success(service.listExceptions(SessionUtil.userId(), SessionUtil.role(), dto));
    }

    //查询单个异常详情
    @GetMapping("/{id}")
    public Result<DeliveryException> detail(@PathVariable Long id) {
        return Result.success(service.exceptionDetail(SessionUtil.userId(), SessionUtil.role(), id));
    }

    //处理配送异常
    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id, @Valid @RequestBody ResolveExceptionDTO dto) {
        service.resolveException(SessionUtil.userId(), SessionUtil.role(), id, dto);
        return Result.success();
    }
}
