package cn.njust.campusexpress.common.util;

import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.enums.UserRoleEnum;

//读取登录会话
public final class SessionUtil {

    //登录会话中角色的键
    public static final String ROLE_KEY = "role";

    private SessionUtil() {
    }

    //读取ID
    public static Long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    //读取角色
    public static UserRoleEnum role() {
        return UserRoleEnum.valueOf((String) StpUtil.getTokenSession().get(ROLE_KEY));
    }
}
