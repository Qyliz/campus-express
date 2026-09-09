package cn.njust.campusexpress.model.user.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员账户，对应 admin 表。仅由 db/data.sql 以 id=0 预置，不开放注册。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Admin extends RoleAccount {
}
