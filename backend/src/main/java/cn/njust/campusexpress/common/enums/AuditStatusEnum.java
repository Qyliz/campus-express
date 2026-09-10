package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 审核记录的状态，与配送员账户是否封禁相互独立；保留原数据库取值。 */
@Getter
@AllArgsConstructor
public enum AuditStatusEnum {
    REVIEWING(2, "审核中"),
    NORMAL(1, "已通过"),
    REJECTED(3, "已驳回");

    @EnumValue
    private final Integer code;
    private final String description;
}
