package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 订单状态：数据库保存数字，接口使用枚举名称。 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    UNPAID(0, "待支付"),
    AVAILABLE(1, "待接单"),
    AWAITING_PICKUP(2, "待揽收"),
    DELIVERING(3, "配送中"),
    AWAITING_COLLECTION(4, "待取件"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消");

    @EnumValue
    private final Integer code;
    private final String description;
}
