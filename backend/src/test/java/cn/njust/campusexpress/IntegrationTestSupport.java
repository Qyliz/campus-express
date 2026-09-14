package cn.njust.campusexpress;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.model.user.entity.Customer;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.service.CustomerService;
import cn.njust.campusexpress.model.user.service.RoleAccountService;
import cn.njust.campusexpress.model.user.service.UserService;
import cn.njust.campusexpress.model.user.service.VerifyCodeService;
import cn.njust.campusexpress.user.RegistrationTestSupport;
import jakarta.servlet.http.Cookie;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试公共支撑：统一的用户 fixture、登录助手与外键安全的清理。
 * 基类不带 @Transactional —— 需要回滚的测试自行标注，需要真实提交的测试（并发/回滚验证）直接复用清理助手。
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    @Autowired protected VerifyCodeService registrationCodes;
    @Autowired protected UserService users;
    @Autowired protected RoleAccountService accounts;
    @Autowired protected CustomerService customers;
    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;

    /** 本测试类创建过的 user id；非事务测试在 @AfterEach 里用 {@link #cleanupCreatedData()} 清掉。 */
    protected final List<Long> createdUserIds = new ArrayList<>();
    /** 本测试类创建过的订单 id，清理时必须先于用户删除（订单对角色账户是 NO ACTION 引用）。 */
    protected final List<Long> createdOrderIds = new ArrayList<>();

    /**
     * 建一个带角色账户的测试用户：手机号走 14 号段随机（见 TestPhones），邮箱随机，
     * 密码统一 test123，可直接凭邮箱登录。
     */
    protected Long createUser(UserRoleEnum role, String username) {
        User user = new User();
        user.setUsername(username);
        user.setGender(UserGenderEnum.UNKNOWN);
        user.setPhone(TestPhones.next());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPassword(BCrypt.hashpw("test123", BCrypt.gensalt(4)));
        users.save(user);
        createdUserIds.add(user.getId());
        accounts.createAccount(user.getId(), role, UserStatusEnum.NORMAL);
        return user.getId();
    }

    /** 通过 HTTP 注册一个收寄件人（NORMAL），并断言 user 主表行与 customer 角色账户行都已创建。 */
    protected Long registerCustomer(String username, String phone) throws Exception {
        mvc.perform(RegistrationTestSupport.registration(registrationCodes)
                        .param("username", username)
                        .param("password", "1234567")
                        .param("role", "CUSTOMER")
                        .param("gender", "MALE")
                        .param("phone", phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        User user = users.lambdaQuery().eq(User::getUsername, username).one();
        assertNotNull(user, "user 主表行应已创建");
        Customer customer = customers.lambdaQuery().eq(Customer::getUserId, user.getId()).one();
        assertNotNull(customer, "收寄件人账户行应已创建");
        return user.getId();
    }

    /** 用 fixture 用户的随机邮箱 + test123 登录，返回 satoken Cookie。 */
    protected Cookie login(Long userId, UserRoleEnum role) throws Exception {
        return login(users.getById(userId).getEmail(), "test123", role.name());
    }

    /** 通过 HTTP 注册一个随机手机号（14 号段）、随机邮箱的收寄件人并登录，返回 satoken Cookie。 */
    protected Cookie registerAndLoginCustomer() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(RegistrationTestSupport.registration(registrationCodes)
                        .param("username", "http测试")
                        .param("password", "1234567")
                        .param("role", "CUSTOMER")
                        .param("gender", "MALE")
                        .param("phone", TestPhones.next())
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        return login(email, "1234567", UserRoleEnum.CUSTOMER.name());
    }

    /** 用任意凭证登录，返回 satoken Cookie（登录失败时为 null，由调用方断言）。 */
    protected Cookie login(String account, String password, String role) throws Exception {
        return mvc.perform(post("/api/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"account\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", account, password, role)))
                .andReturn().getResponse().getCookie("satoken");
    }

    /**
     * 非事务测试的 @AfterEach 清理，顺序由外键决定：
     * 先删订单（级联清掉流转记录/异常/评价/申诉），再删用户（级联清掉角色账户与审核/封禁记录）。
     */
    protected void cleanupCreatedData() {
        for (Long id : createdOrderIds) {
            jdbc.update("delete from express_order where id = ?", id);
        }
        for (Long id : createdUserIds) {
            jdbc.update("delete from user where id = ?", id);
        }
    }
}
