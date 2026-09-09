package cn.njust.campusexpress.model.user.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配送员账户，对应 courier 表。注册后需管理员审核，审核材料见 user_audit_record。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Courier extends RoleAccount {
}
