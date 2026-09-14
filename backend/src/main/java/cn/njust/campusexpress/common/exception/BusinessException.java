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

    /** PARAM_ERROR 的简写工厂，业务校验失败时使用，调用方静态导入后直接写 invalid("提示")。 */
    public static BusinessException invalid(String message) {
        return new BusinessException(ResultCodeEnum.PARAM_ERROR, message);
    }
}
