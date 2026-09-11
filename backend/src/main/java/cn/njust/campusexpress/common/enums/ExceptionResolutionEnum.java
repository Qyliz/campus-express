package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 管理员对配送异常的处理结果：数据库保存数字，接口使用枚举名称。 */
@Getter
@AllArgsConstructor
public enum ExceptionResolutionEnum {
    RESUME(0, "恢复配送"),
    CANCEL(1, "取消订单");

    @EnumValue
    private final Integer code;
    private final String description;
}
