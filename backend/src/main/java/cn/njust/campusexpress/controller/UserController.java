package cn.njust.campusexpress.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.dto.*;
import cn.njust.campusexpress.service.UserAuditRecordService;
import cn.njust.campusexpress.service.UserBanRecordService;
import cn.njust.campusexpress.service.UserService;
import cn.njust.campusexpress.vo.UserAuditRecordVO;
import cn.njust.campusexpress.vo.UserProfileAdminVO;
import cn.njust.campusexpress.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserAuditRecordService userAuditRecordService;
    private final UserBanRecordService userBanRecordService;

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
            throw new BusinessException(ResultCodeEnum.PARAM_MISSING);
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
            throw new BusinessException(ResultCodeEnum.PARAM_MISSING);
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

    //获取审核记录
    @GetMapping("/audit")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Page<UserAuditRecordVO>> getAuditRecord(@Valid @RequestBody UserAuditQueryDTO dto) {
        Page<UserAuditRecordVO> page = userAuditRecordService.getRecordPage(dto);
        return Result.success(page);
    }

    //审核账号
    @PutMapping("/audit")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> auditUser(@Valid @RequestBody UserAuditDTO dto) {
        userAuditRecordService.auditUser(dto);
        return Result.success();
    }

    //获取所有账号信息
    @GetMapping("/all-users")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Page<UserProfileAdminVO>> getAllUsers(@Valid @RequestBody UserQueryDTO dto) {
        Page<UserProfileAdminVO> page = userService.getAllUsers(dto);
        return Result.success(page);
    }

    //封禁账号
    @PostMapping("/ban")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> banUser(@Valid @RequestBody UserBanDTO dto) {
        userBanRecordService.banUser(dto);
        return Result.success();
    }
    //TODO: 忘记密码、找回用户名、验证码、账户审核图片版
}