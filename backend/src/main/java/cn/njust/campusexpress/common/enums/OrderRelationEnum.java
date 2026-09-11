package cn.njust.campusexpress.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 当前收寄件人与订单的关系，仅用于接口筛选，不对应数据库字段。 */
@Getter
@AllArgsConstructor
public enum OrderRelationEnum {
    ALL("全部订单"),
    CREATED("我下的"),
    RECEIVED("我收到的");

    private final String description;
}
