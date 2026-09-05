package cn.njust.campusexpress.user;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UpdateInfoTest {

    @Autowired
    private MockMvc mockMvc;

    private Cookie cookie;

    @BeforeEach
    void login() throws Exception {

        String json = """
                {
                    "account":"admin@email.com",
                    "password":"IamADMIN",
                    "role":"ADMIN"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andReturn();
        cookie = result.getResponse()
                .getCookie("satoken");
    }

    @Test
    void logoutTest() throws Exception {
        mockMvc.perform(post("/api/user/logout")
                .cookie(cookie)
        ).andExpect(status().isOk()
        ).andExpect(jsonPath("$.code")
                .value(ResultCodeEnum.SUCCESS.getCode()));
    }

    @Test
    void updateUsernameTest() throws Exception {
        //修改成功
        String json1 = """
                {
                    "username":"aaa"
                }
                """;
        mockMvc.perform(put("/api/user/username")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json1)
        ).andExpect(status().isOk()
        ).andExpect(jsonPath("$.code")
                .value(ResultCodeEnum.SUCCESS.getCode())
        ).andExpect(jsonPath("$.data.username")
                .value("aaa"));
        //缺少参数
        String json2 = """
                {}
                """;
        mockMvc.perform(put("/api/user/username")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json2)
        ).andExpect(status().isOk()
        ).andExpect(jsonPath("$.code")
                .value(ResultCodeEnum.PARAM_MISSING.getCode()));

        //错误参数
        String json3 = """
                {
                    "username":"+-*/abcd"
                }
                """;
        mockMvc.perform(put("/api/user/username")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json3)
        ).andExpect(status().isOk()
        ).andExpect(jsonPath("$.code")
                .value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    @Test
    void updateGenderTest() throws Exception {
        //修改成功
        String json1 = """
                {
                    "gender":"MALE"
                }
                """;
        mockMvc.perform(put("/api/user/gender")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json1)
        ).andExpect(status().isOk()
        ).andExpect(jsonPath("$.code")
                .value(ResultCodeEnum.SUCCESS.getCode())
        ).andExpect(jsonPath("$.data.gender")
                .value("MALE"));
        //缺少参数
        String json2 = """
                {}
                """;
        mockMvc.perform(put("/api/user/gender")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json2)
        ).andExpect(status().isOk()
        ).andExpect(jsonPath("$.code")
                .value(ResultCodeEnum.PARAM_MISSING.getCode()));

        //错误参数
        String json3 = """
                {
                    "gender":"IDK"
                }
                """;
        mockMvc.perform(put("/api/user/gender")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json3)
        ).andExpect(status().isOk()
        ).andExpect(jsonPath("$.code")
                .value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    @Test
    void updateAvatar() throws Exception {
        //成功修改
        byte[] image = Files.readAllBytes(Paths.get("upload/avatar/testAvatar.png"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "testAvatar.png",
                "image/png",
                image);
        mockMvc.perform(multipart("/api/user/avatar")
                        .file(file)
                        .cookie(cookie)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").exists());
        //文件为空
        file = new MockMultipartFile(
                "file",
                "testAvatar.png",
                "image/png",
                new byte[0]);
        mockMvc.perform(multipart("/api/user/avatar")
                        .file(file)
                        .cookie(cookie)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").
                        value(ResultCodeEnum.FILE_EMPTY.getCode()));
    }
}
