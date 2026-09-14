package cn.njust.campusexpress.common.util;

import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.enums.UserRoleEnum;

/** 登录会话读取的统一入口：常量化 token-session 的键，避免魔法字符串散落各处。 */
public final class SessionUtil {

    /** token-session 中保存登录时所选角色枚举名的键，StpInterfaceImpl 的权限判定依赖同一个键。 */
    public static final String ROLE_KEY = "role";

    private SessionUtil() {
    }

    /** 当前登录用户 id（即 user 表主键）。 */
    public static Long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    /** 当前会话登录时所选的角色，由 login 写入 token session。 */
    public static UserRoleEnum role() {
        return UserRoleEnum.valueOf((String) StpUtil.getTokenSession().get(ROLE_KEY));
    }
}
