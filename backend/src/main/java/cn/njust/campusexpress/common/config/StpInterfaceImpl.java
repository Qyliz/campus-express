package cn.njust.campusexpress.common.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String role = (String) StpUtil.getSession().get("role");
        if (role == null) {
            return List.of();
        }
        if (role.equals(UserRoleEnum.ROLE_ADMIN)) {
            return List.of(UserRoleEnum.ROLE_ADMIN, UserRoleEnum.ROLE_CUSTOMER, UserRoleEnum.ROLE_COURIER);
        }
        return List.of(role);
    }
}
