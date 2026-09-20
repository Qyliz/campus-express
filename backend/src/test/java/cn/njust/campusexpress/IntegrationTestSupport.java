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

//提供集成测试共用的用户、登录和数据清理方法
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    @Autowired protected VerifyCodeService registrationCodes;
    @Autowired protected UserService users;
    @Autowired protected RoleAccountService accounts;
    @Autowired protected CustomerService customers;
    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;

    //记录非事务测试创建的用户
    protected final List<Long> createdUserIds = new ArrayList<>();
    //记录非事务测试创建的订单
    protected final List<Long> createdOrderIds = new ArrayList<>();

    //创建带指定角色账户的测试用户
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

    //通过接口注册收寄件人并校验用户和角色账户均已创建
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

    //使用测试用户凭证登录并返回会话Cookie
    protected Cookie login(Long userId, UserRoleEnum role) throws Exception {
        return login(users.getById(userId).getEmail(), "test123", role.name());
    }

    //注册并登录随机收寄件人
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

    //使用指定凭证登录并返回会话Cookie
    protected Cookie login(String account, String password, String role) throws Exception {
        return mvc.perform(post("/api/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"account\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", account, password, role)))
                .andReturn().getResponse().getCookie("satoken");
    }

    //按外键依赖顺序清理订单和用户测试数据
    protected void cleanupCreatedData() {
        for (Long id : createdOrderIds) {
            jdbc.update("delete from express_order where id = ?", id);
        }
        for (Long id : createdUserIds) {
            jdbc.update("delete from user where id = ?", id);
        }
    }
}
