package cn.njust.campusexpress.model.user.entity;

import cn.njust.campusexpress.common.enums.UserRoleEnum;
import lombok.Data;

import java.util.Date;


/**
 * 封禁记录。角色账户分散在 customer / courier / admin 三张表，
 * 因此这里用 user_id + role 两列做多态引用，只有 user_id 上有外键。
 */
@Data
public class UserBanRecord {
    private Long id;
    private Long userId;
    private UserRoleEnum role;
    private Integer unbanned;
    private String reason;
    private Date createTime;
}
