package cn.njust.campusexpress.common.exception.handler;

import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@SuppressWarnings("unused")
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FileExceptionHandler {
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingFile(
            MissingServletRequestPartException e) {

        return Result.fail(ResultCodeEnum.FILE_EMPTY);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(
            MaxUploadSizeExceededException e) {
        return Result.fail(ResultCodeEnum.FILE_TOO_LARGE);
    }
}
