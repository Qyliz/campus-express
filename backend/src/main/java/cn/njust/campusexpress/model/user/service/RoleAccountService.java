package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.model.user.entity.RoleAccount;

/**
 * 角色账户路由器：按 {@link UserRoleEnum} 把调用分发到 customer / courier / admin 三张同构表，
 * 避免「按角色三选一」的 switch 散落在注册、登录、注销、封禁、解封各处。
 */
public interface RoleAccountService {

    /**
     * 查询某用户在指定角色下的账户
     *
     * @return 没有该角色账户时返回 null
     */
    RoleAccount getByUserAndRole(Long userId, UserRoleEnum role);

    /**
     * 为某用户新建一个角色账户
     *
     * @return 已落库（含雪花 id）的账户对象
     */
    RoleAccount createAccount(Long userId, UserRoleEnum role, UserStatusEnum status);

    /**
     * 更新账户状态，account 必须是由 {@link #getByUserAndRole} 按同一 role 取出的对象
     */
    void updateStatus(RoleAccount account, UserRoleEnum role, UserStatusEnum status);

    /**
     * 逻辑删除某用户的指定角色账户（注销）
     *
     * @return 账户不存在时返回 false
     */
    boolean deleteByUserAndRole(Long userId, UserRoleEnum role);
}
