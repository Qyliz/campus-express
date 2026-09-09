package cn.njust.campusexpress.model.user.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收寄件人账户，对应 customer 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Customer extends RoleAccount {
}
