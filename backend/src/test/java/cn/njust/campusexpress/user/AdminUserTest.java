package cn.njust.campusexpress.user;

import cn.njust.campusexpress.IntegrationTestSupport;
import cn.njust.campusexpress.TestAccounts;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminUserTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private Cookie adminCookie;

    @BeforeEach
    void adminLogin() throws Exception {
        //使用种子管理员凭证登录
        adminCookie = login(TestAccounts.ADMIN_EMAIL, TestAccounts.ADMIN_PASSWORD, "ADMIN");
        Assertions.assertNotNull(adminCookie, "管理员登录应返回 satoken");
    }

    //管理员分页查询所有角色账号并绑定查询条件
    @Test
    void getAllUsersOk() throws Exception {
        mockMvc.perform(get("/api/user/all-users")
                        .param("currentPage", "1")
                        .param("sort", "CREATE_TIME_DESC")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    //非法页码返回参数错误
    @Test
    void getAllUsersInvalidPage() throws Exception {
        mockMvc.perform(get("/api/user/all-users")
                        .param("currentPage", "0")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    //多角色用户在管理列表中按角色分行并正确参与总数统计
    @Test
    void getAllUsersCountsOneRowPerRole() throws Exception {
        mockMvc.perform(cn.njust.campusexpress.user.RegistrationTestSupport.registration(registrationCodes)
                        .param("username", "dual_role")
                        .param("password", "1234567")
                        .param("role", "CUSTOMER")
                        .param("gender", "MALE")
                        .param("phone", "13900000601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //使用相同手机号和密码追加配送员角色且不覆盖用户资料
        mockMvc.perform(cn.njust.campusexpress.user.RegistrationTestSupport.registration(registrationCodes)
                        .file(new MockMultipartFile("material", "m.png", "image/png", new byte[]{1, 2, 3}))
                        .param("username", "ignored")
                        .param("password", "1234567")
                        .param("role", "COURIER")
                        .param("gender", "FEMALE")
                        .param("phone", "13900000601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        mockMvc.perform(get("/api/user/all-users")
                        .param("currentPage", "1")
                        .param("username", "dual_role")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[*].role",
                        org.hamcrest.Matchers.containsInAnyOrder("CUSTOMER", "COURIER")))
                .andExpect(jsonPath("$.data.records[*].username",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("dual_role"))));
    }

    //管理员分页查询审核记录
    @Test
    void getAuditRecordOk() throws Exception {
        mockMvc.perform(get("/api/user/audit")
                        .param("currentPage", "1")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    //收寄件人访问管理端接口时返回无权限
    @Test
    void customerAccessDenied() throws Exception {
        registerCustomer("cus_denied", "13900000101");
        Cookie customerCookie = login("13900000101", "1234567", "CUSTOMER");
        mockMvc.perform(get("/api/user/all-users")
                        .param("currentPage", "1")
                        .cookie(customerCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.NO_PERMISSION.getCode()));
    }

    //验证封禁踢出会话、记录查询和解封恢复登录的完整流程
    @Test
    void banKickoutAndUnban() throws Exception {
        Long userId = registerCustomer("cus_ban", "13900000102");
        Cookie customerCookie = login("13900000102", "1234567", "CUSTOMER");

        //封禁前可正常访问个人资料
        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //管理员封禁
        mockMvc.perform(post("/api/user/{userId}/roles/CUSTOMER/ban", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"违规操作\"}")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //被封禁账号的旧会话返回已被踢下线
        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.KICKED_OUT.getCode()));

        //封禁记录可查（至少一条）
        mockMvc.perform(get("/api/user/ban")
                        .param("currentPage", "1")
                        .param("username", "cus_ban")
                        .param("unbanned", "false")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].unbanned").value(false));

        //解封
        mockMvc.perform(post("/api/user/{userId}/roles/CUSTOMER/unban", userId)
                        .contentType(MediaType.APPLICATION_JSON)

                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        mockMvc.perform(get("/api/user/ban")
                        .param("currentPage", "1")
                        .param("username", "cus_ban")
                        .param("unbanned", "true")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.records[0].unbanned").value(true));

        //解封后可重新登录，说明状态恢复为 NORMAL
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000102\",\"password\":\"1234567\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
    }

    //解封未被封禁的账号时返回账号未封禁
    @Test
    void unbanNotBannedAccount() throws Exception {
        Long userId = registerCustomer("cus_notban", "13900000103");
        mockMvc.perform(post("/api/user/{userId}/roles/CUSTOMER/unban", userId)
                        .contentType(MediaType.APPLICATION_JSON)

                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.ACCOUNT_NOT_BANNED.getCode()));
    }

    //封禁不存在的账号时返回用户不存在
    @Test
    void banNonexistentUser() throws Exception {
        Long userId = 999999999999L;
        mockMvc.perform(post("/api/user/{userId}/roles/CUSTOMER/ban", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"x\"}")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.USER_NOT_FOUND.getCode()));
    }

    //审核结果不是通过或驳回时返回参数错误
    @Test
    void auditInvalidStatus() throws Exception {
        mockMvc.perform(put("/api/user/audit/12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEWING\"}")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    //审核不存在的记录时返回用户不存在
    @Test
    void auditValidStatusNotFound() throws Exception {
        mockMvc.perform(put("/api/user/audit/12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NORMAL\"}")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.USER_NOT_FOUND.getCode()));
    }

    //收寄件人注册不产生审核记录而配送员注册产生一条
    @Test
    void auditRecordOnlyForReviewRoles() throws Exception {
        mockMvc.perform(cn.njust.campusexpress.user.RegistrationTestSupport.registration(registrationCodes)
                        .param("username", "cust_noaud")
                        .param("password", "1234567")
                        .param("role", "CUSTOMER")
                        .param("gender", "MALE")
                        .param("phone", "13900000302"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        mockMvc.perform(get("/api/user/audit")
                        .param("username", "cust_noaud")
                        .param("currentPage", "1")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        MockMultipartFile material = new MockMultipartFile("material", "m.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(cn.njust.campusexpress.user.RegistrationTestSupport.registration(registrationCodes)
                        .file(material)
                        .param("username", "cour_audit")
                        .param("password", "1234567")
                        .param("role", "COURIER")
                        .param("gender", "MALE")
                        .param("phone", "13900000303"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        mockMvc.perform(get("/api/user/audit")
                        .param("username", "cour_audit")
                        .param("currentPage", "1")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].material").isNotEmpty());
    }

    //配送员注册未提交材料时返回文件为空
    @Test
    void courierRegisterRequiresMaterial() throws Exception {
        mockMvc.perform(cn.njust.campusexpress.user.RegistrationTestSupport.registration(registrationCodes)
                        .param("username", "cour_nomat")
                        .param("password", "1234567")
                        .param("role", "COURIER")
                        .param("gender", "MALE")
                        .param("phone", "13900000304"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.FILE_EMPTY.getCode()));
    }

    //管理员强制下线后目标账号的旧会话返回已被踢下线
    @Test
    void adminKickoutUser() throws Exception {
        Long userId = registerCustomer("cus_kick", "13900000301");
        Cookie customerCookie = login("13900000301", "1234567", "CUSTOMER");

        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        mockMvc.perform(post("/api/user/{userId}/kickout", userId)
                        .contentType(MediaType.APPLICATION_JSON)

                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.KICKED_OUT.getCode()));
    }

    //管理员重置密码后旧会话被踢出且仅新密码可登录
    @Test
    void adminResetPassword() throws Exception {
        Long userId = registerCustomer("cus_reset", "13900000501");
        Cookie customerCookie = login("13900000501", "1234567", "CUSTOMER");

        mockMvc.perform(post("/api/user/{userId}/reset-password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"adminset1\"}")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //旧会话被踢
        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.KICKED_OUT.getCode()));

        //新密码可登录
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000501\",\"password\":\"adminset1\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //旧密码失效
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000501\",\"password\":\"1234567\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.LOGIN_ERROR.getCode()));
    }
}
