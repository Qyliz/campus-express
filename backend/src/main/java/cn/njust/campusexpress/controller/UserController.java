package cn.njust.campusexpress.controller;

import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.dto.UserLoginDTO;
import cn.njust.campusexpress.dto.UserRegisterDTO;
import cn.njust.campusexpress.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    Result<Void> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    @PostMapping("/login")
    Result<Void> login(@Valid @RequestBody UserLoginDTO loginDTO, HttpSession session) {
        Long userId = userService.login(loginDTO);
        session.setAttribute("userId", userId);
        return Result.success();
    }
}
