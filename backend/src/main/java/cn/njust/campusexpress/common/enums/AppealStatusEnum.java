package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 评价申诉状态：数据库保存数字，接口使用枚举名称。 */
@Getter
@AllArgsConstructor
public enum AppealStatusEnum {
    PENDING(0, "待处理"),
    UPHELD(1, "申诉成立"),
    REJECTED(2, "已驳回");

    @EnumValue
    private final Integer code;
    private final String description;
}
