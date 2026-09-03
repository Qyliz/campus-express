package cn.njust.campusexpress.common.exception;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final Integer code;

    public BaseException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BaseException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
