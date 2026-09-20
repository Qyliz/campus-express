package cn.njust.campusexpress.model.user.entity;

import cn.njust.campusexpress.common.enums.UserRoleEnum;
import lombok.Data;

import java.util.Date;

@Data
public class UserBanRecord {
    private Long id;
    private Long userId;
    private UserRoleEnum role;
    private Boolean unbanned;
    private String reason;
    private Date createTime;
}
