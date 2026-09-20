package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.model.user.entity.RoleAccount;

//角色账户路由Service
public interface RoleAccountService {

    //查询用户的指定角色账户
    RoleAccount getByUserAndRole(Long userId, UserRoleEnum role);

    //为用户创建指定角色账户
    RoleAccount createAccount(Long userId, UserRoleEnum role, UserStatusEnum status);

    //更新指定角色账户状态
    void updateStatus(RoleAccount account, UserRoleEnum role, UserStatusEnum status);

    //逻辑删除用户的指定角色账户
    boolean deleteByUserAndRole(Long userId, UserRoleEnum role);
}
