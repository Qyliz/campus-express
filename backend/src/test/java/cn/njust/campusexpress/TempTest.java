package cn.njust.campusexpress;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.dto.UserRegisterDTO;
import cn.njust.campusexpress.entity.User;
import cn.njust.campusexpress.mapper.UserMapper;
import cn.njust.campusexpress.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TempTest {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UserService userService;

    @Test
    void test01() {
        User user = new User();
        user.setPhone("123");
        user.setEmail("123");
        user.setPassword("123");
        userMapper.insert(user);
    }

    @Test
    void test02() {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("abcde");
        registerDTO.setPassword("12345678");
        registerDTO.setPhone("13788888888");
        registerDTO.setEmail("123456@email.com");
        registerDTO.setRole(UserRoleEnum.CUSTOMER);
        registerDTO.setGender(UserGenderEnum.FEMALE);
        userService.register(registerDTO);
    }
}
