package cn.njust.campusexpress.model.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.user.dto.*;
import cn.njust.campusexpress.model.user.service.UserAuditRecordService;
import cn.njust.campusexpress.model.user.service.UserBanRecordService;
import cn.njust.campusexpress.model.user.service.UserService;
import cn.njust.campusexpress.model.user.service.VerifyCodeService;
import cn.njust.campusexpress.model.user.vo.UserAuditRecordVO;
import cn.njust.campusexpress.model.user.vo.UserBanRecordVO;
import cn.njust.campusexpress.model.user.vo.UserProfileAdminVO;
import cn.njust.campusexpress.model.user.vo.UserProfileVO;
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
    private final VerifyCodeService verifyCodeService;

    //注册（手机号或邮箱已存在且密码正确时，为本人追加一个新角色）
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<Void> register(@Valid @ModelAttribute UserRegisterDTO registerDTO,
                          @RequestPart(value = "material", required = false) MultipartFile material) {
        userService.register(registerDTO, material);
        return Result.success();
    }

    //登录：loginId 即 user.id，本次登录所选的角色存入 token session
    @PostMapping("/login")
    Result<Void> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        Long userId = userService.login(loginDTO);
        StpUtil.login(userId);
        StpUtil.getTokenSession().set("role", loginDTO.getRole().name());
        return Result.success();
    }


    //登出账号
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
        UserProfileVO profile = userService.getProfile(StpUtil.getLoginIdAsLong(), currentRole());
        return Result.success(profile);
    }

    //更新用户名
    @PutMapping("/username")
    @SaCheckLogin
    Result<UserProfileVO> updateUsername(@Valid @RequestBody UserUsernameDTO dto) {
        String username = dto.getUsername();
        if (username == null || username.isBlank()) {
            throw new BusinessException(ResultCodeEnum.PARAM_MISSING);
        }
        UserProfileVO profile = userService.updateUsername(StpUtil.getLoginIdAsLong(), currentRole(), username);
        return Result.success(profile);
    }

    //更新性别
    @PutMapping("/gender")
    @SaCheckLogin
    Result<UserProfileVO> updateGender(@Valid @RequestBody UserGenderDTO dto) {
        UserGenderEnum gender = dto.getGender();
        if (gender == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_MISSING);
        }
        UserProfileVO profile = userService.updateGender(StpUtil.getLoginIdAsLong(), currentRole(), gender);
        return Result.success(profile);
    }

    //更新头像
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckLogin
    Result<UserProfileVO> updateAvatar(@RequestParam("file") MultipartFile file) {
        UserProfileVO profile = userService.updateAvatar(StpUtil.getLoginIdAsLong(), currentRole(), file);
        return Result.success(profile);
    }

    //修改密码
    @PutMapping("/password")
    @SaCheckLogin
    Result<Void> updatePassword(@Valid @RequestBody UserPasswordDTO dto) {
        userService.updatePassword(StpUtil.getLoginIdAsLong(), dto);
        return Result.success();
    }

    //注销账号（仅注销当前角色，user 主表与其他角色账户保留）
    @DeleteMapping("/account")
    @SaCheckLogin
    Result<Void> deleteAccount() {
        userService.deleteAccount(StpUtil.getLoginIdAsLong(), currentRole());
        return Result.success();
    }

    //发送验证码（桩版：直接返回验证码，由前端展示以模拟发送）
    @PostMapping("/verify-code")
    Result<String> sendVerifyCode(@Valid @RequestBody SendCodeDTO dto) {
        String code = verifyCodeService.send(dto.getAccount(), dto.getScene());
        return Result.success(code);
    }

    //忘记密码：凭验证码重置密码
    @PostMapping("/reset-password")
    Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return Result.success();
    }

    //换绑手机号
    @PutMapping("/phone")
    @SaCheckLogin
    Result<Void> updatePhone(@Valid @RequestBody ChangePhoneDTO dto) {
        userService.updatePhone(StpUtil.getLoginIdAsLong(), dto);
        return Result.success();
    }

    //换绑邮箱
    @PutMapping("/email")
    @SaCheckLogin
    Result<Void> updateEmail(@Valid @RequestBody ChangeEmailDTO dto) {
        userService.updateEmail(StpUtil.getLoginIdAsLong(), dto);
        return Result.success();
    }

    //获取审核记录
    @GetMapping("/audit")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<PageResult<UserAuditRecordVO>> getAuditRecord(@Valid UserAuditQueryDTO dto) {
        Page<UserAuditRecordVO> page = userAuditRecordService.getRecordPage(dto);
        return Result.success(PageResult.of(page));
    }

    //审核账号
    @PutMapping("/audit")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> auditUser(@Valid @RequestBody UserAuditDTO dto) {
        userAuditRecordService.auditUser(dto);
        return Result.success();
    }

    //获取所有账号信息（一人持多角色时，每个角色各占一行）
    @GetMapping("/all-users")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<PageResult<UserProfileAdminVO>> getAllUsers(@Valid UserQueryDTO dto) {
        Page<UserProfileAdminVO> page = userService.getAllUsers(dto);
        return Result.success(PageResult.of(page));
    }

    //封禁账号
    @PostMapping("/ban")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> banUser(@Valid @RequestBody UserBanDTO dto) {
        userBanRecordService.banUser(dto);
        //踢出被禁用户的在线会话，其后续请求将返回 KICKED_OUT
        StpUtil.kickout(dto.getUserId());
        return Result.success();
    }

    //获取封禁记录
    @GetMapping("/ban")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<PageResult<UserBanRecordVO>> getBanRecord(@Valid UserBanQueryDTO dto) {
        Page<UserBanRecordVO> page = userBanRecordService.getBanRecordPage(dto);
        return Result.success(PageResult.of(page));
    }

    //解封账号
    @PostMapping("/unban")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> unbanUser(@Valid @RequestBody UserUnbanDTO dto) {
        userBanRecordService.unbanUser(dto);
        return Result.success();
    }

    //强制下线
    @PostMapping("/kickout")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> kickoutUser(@Valid @RequestBody UserKickoutDTO dto) {
        StpUtil.kickout(dto.getUserId());
        return Result.success();
    }

    //管理员重置他人密码
    @PostMapping("/admin/reset-password")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> adminResetPassword(@Valid @RequestBody AdminResetPasswordDTO dto) {
        userService.adminResetPassword(dto);
        return Result.success();
    }

    //当前会话登录时所选的角色，由 login 写入 token session；StpInterfaceImpl 也依赖同一个键做权限判定
    private UserRoleEnum currentRole() {
        return UserRoleEnum.valueOf((String) StpUtil.getTokenSession().get("role"));
    }
}
