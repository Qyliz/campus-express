package cn.njust.campusexpress.common.exception;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends BaseException {
    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode);
    }

    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(resultCode, message);
    }
}
