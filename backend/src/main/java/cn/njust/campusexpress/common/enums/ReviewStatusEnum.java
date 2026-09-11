package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 服务评价状态：申诉成立时作废该评价。数据库保存数字，接口使用枚举名称。 */
@Getter
@AllArgsConstructor
public enum ReviewStatusEnum {
    VALID(0, "有效"),
    VOID(1, "已作废");

    @EnumValue
    private final Integer code;
    private final String description;
}
