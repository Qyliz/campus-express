package cn.njust.campusexpress.common.exception.handler;

import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order
public class GlobalExceptionHandler {

    // 路径或查询参数中的 ID、角色等格式无效时，返回参数错误而不是系统错误。
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR, "参数格式不正确：" + e.getName());
    }

    //处理参数反序列异常
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handle(HttpMessageNotReadableException e) {
        log.warn("参数反序列异常: {}", e.getMessage());
        return Result.fail(ResultCodeEnum.PARAM_ERROR);
    }

    //处理参数校验异常（涵盖请求体与查询参数两种绑定校验）
    @ExceptionHandler(BindException.class)
    public Result<Void> handleValidException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> (error.getField() + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", message);
        return Result.fail(ResultCodeEnum.PARAM_ERROR, message);
    }

    //处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e);
    }

    //处理未知异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统内部异常: ", e);
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR);
    }
}
