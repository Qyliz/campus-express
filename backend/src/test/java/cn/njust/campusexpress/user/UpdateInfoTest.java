package cn.njust.campusexpress.user;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UpdateInfoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void test01() throws Exception {
        String json = """
                {
                    "account":"admin@email.com",
                    "password":"IamADMIN",
                    "role":"ADMIN"
                }
                """;
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(ResultCodeEnum.SUCCESS.getCode())
                );
    }
}
