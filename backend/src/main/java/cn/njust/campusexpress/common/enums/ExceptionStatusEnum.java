package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 配送异常处理状态：数据库保存数字，接口使用枚举名称。 */
@Getter
@AllArgsConstructor
public enum ExceptionStatusEnum {
    PENDING(0, "待处理"),
    RESOLVED(1, "已处理");

    @EnumValue
    private final Integer code;
    private final String description;
}
