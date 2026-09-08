package cn.njust.campusexpress.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("unused")
@Getter
@AllArgsConstructor
public enum SortEnum {
    CREATE_TIME_ASC(0, "创建时间升序"),
    CREATE_TIME_DESC(1, "创建时间降序"),
    UPDATE_TIME_ASC(2, "更新时间升序"),
    UPDATE_TIME_DESC(3, "更新时间降序");

    private final Integer code;
    private final String message;
}
