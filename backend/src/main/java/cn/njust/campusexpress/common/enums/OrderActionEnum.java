package cn.njust.campusexpress.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


//订单业务动作，Controller 端点与服务层之间传递的枚举
//code 是订单详情 allowedActions 返回给前端的字符串，也是 URL 里的动作名。

@Getter
@AllArgsConstructor
public enum OrderActionEnum {
    PAY("pay", "支付订单"),
    ACCEPT("accept", "接单"),
    PICKUP("pickup", "确认揽收"),
    DELIVER("deliver", "确认送达"),
    COMPLETE("complete", "确认取件"),
    CANCEL("cancel", "收寄件人取消"),
    ADMIN_CANCEL("admin-cancel", "管理员取消");

    private final String code;
    private final String description;
}
