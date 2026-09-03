package cn.njust.campusexpress.common.exception.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class SaTokenExceptionHandler {

    //处理未登录异常
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        ResultCodeEnum codeEnum;
        String type = e.getType();
        if (type.equals(NotLoginException.BE_REPLACED)) {
            codeEnum = ResultCodeEnum.BE_REPLACED;
        } else if (type.equals(NotLoginException.KICK_OUT)) {
            codeEnum = ResultCodeEnum.KICKED_OUT;
        } else {
            codeEnum = ResultCodeEnum.UNAUTHORIZED;
        }
        log.warn("未登录异常: {}", codeEnum.getMessage());
        return Result.fail(codeEnum);
    }

    //处理角色认证异常
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("角色认证异常: {}", e.getMessage());
        return Result.fail(ResultCodeEnum.NO_PERMISSION);
    }
}
