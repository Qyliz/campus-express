package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 配送异常类型：数据库保存数字，接口使用枚举名称。 */
@Getter
@AllArgsConstructor
public enum ExceptionTypeEnum {
    CONTACT(0, "联系不上"),
    ADDRESS(1, "地址问题"),
    ITEM(2, "物品问题"),
    COURIER(3, "配送员突发情况"),
    OTHER(4, "其他");

    @EnumValue
    private final Integer code;
    private final String description;
}
