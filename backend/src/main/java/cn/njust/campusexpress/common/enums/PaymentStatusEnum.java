package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 支付状态：数据库保存数字，接口使用枚举名称。 */
@Getter
@AllArgsConstructor
public enum PaymentStatusEnum {
    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款");

    @EnumValue
    private final Integer code;
    private final String description;
}
