package cn.njust.campusexpress;

/** 测试与种子数据的公共约定：管理员凭证由 db/data.sql 播种，多个测试类都要凭它登录。 */
public final class TestAccounts {

    /** data.sql 种子管理员的登录邮箱。 */
    public static final String ADMIN_EMAIL = "admin@email.com";

    /** data.sql 种子管理员的登录密码（所有种子账号同款 BCrypt 哈希）。 */
    public static final String ADMIN_PASSWORD = "IamADMIN";

    private TestAccounts() {
    }
}
