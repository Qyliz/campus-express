package cn.njust.campusexpress.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.dto.UserLoginDTO;
import cn.njust.campusexpress.dto.UserRegisterDTO;
import cn.njust.campusexpress.dto.UserUpdateDTO;
import cn.njust.campusexpress.service.UserService;
import cn.njust.campusexpress.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    //注册
    @PostMapping("/register")
    Result<Void> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    //登录
    @PostMapping("/login")
    Result<Void> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        Long userRoleId = userService.login(loginDTO);
        StpUtil.login(userRoleId);
        StpUtil.getTokenSession().set("role", loginDTO.getRole().name());
        return Result.success();
    }


    //注销
    @PostMapping("/logout")
    @SaCheckLogin
    Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    //获取账号信息
    @GetMapping("/profile")
    @SaCheckLogin
    Result<UserProfileVO> getProfile() {
        Long userRoleId = StpUtil.getLoginIdAsLong();
        UserProfileVO profile = userService.getProfile(userRoleId);
        return Result.success(profile);
    }

    //更新用户名
    @PutMapping("/username")
    @SaCheckLogin
    Result<UserProfileVO> updateUsername(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        String username = userUpdateDTO.getUsername();
        if (username == null || username.isBlank()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
        Long userRoleId = StpUtil.getLoginIdAsLong();
        UserProfileVO profile = userService.updateUsername(userRoleId, username);
        return Result.success(profile);
    }

    //更新性别
    @PutMapping("/gender")
    @SaCheckLogin
    Result<UserProfileVO> updateGender(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        UserGenderEnum gender = userUpdateDTO.getGender();
        if (gender == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
        Long userRoleId = StpUtil.getLoginIdAsLong();
        UserProfileVO profile = userService.updateGender(userRoleId, gender);
        return Result.success(profile);
    }

    //更新头像
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckLogin
    Result<UserProfileVO> updateAvatar(@RequestParam("file") MultipartFile file) {
        Long userRoleId = StpUtil.getLoginIdAsLong();
        UserProfileVO profile = userService.updateAvatar(userRoleId, file);
        return Result.success(profile);
    }
    //TODO: 账号信息维护、封禁账号、账号审核、验证码
}