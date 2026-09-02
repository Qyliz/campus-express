package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {
    DISABLED(0, "禁用"),
    NORMAL(1, "正常"),
    REVIEWING(2, "审核中"),
    REJECTED(3, "审核驳回");

    @EnumValue
    private final Integer code;
    private final String description;
}
