package cn.njust.campusexpress.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

//订单列表范围，每个值对应 OrderController 的一个查询端点
@Getter
@AllArgsConstructor
public enum OrderListScopeEnum {
    MINE("当前收寄件人相关订单"),
    AVAILABLE("接单大厅待接单订单"),
    ASSIGNED("配送员已接任务"),
    ADMIN("管理员全量订单");

    private final String description;
}
