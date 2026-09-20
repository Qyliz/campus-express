package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.user.entity.RoleAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

//角色账户守卫，统一角色匹配 + 账户存在且可用的校验，供订单、评价等模块复用
@Component
@RequiredArgsConstructor
public class AccountGuard {

    private final RoleAccountService accounts;

    //校验当前身份与期望角色一致，且该角色账户存在并处于正常状态
    public RoleAccount requireRole(Long userId, UserRoleEnum role, UserRoleEnum expected) {
        if (role != expected) {
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
        }
        return requireActiveAccount(userId, role);
    }

    //不限制具体角色，只要求当前身份的账户存在并处于正常状态
    public RoleAccount requireActiveAccount(Long userId, UserRoleEnum role) {
        RoleAccount account = accounts.getByUserAndRole(userId, role);
        if (account == null || account.getStatus() != UserStatusEnum.NORMAL) {
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION, "当前角色账户不可用");
        }
        return account;
    }
}
