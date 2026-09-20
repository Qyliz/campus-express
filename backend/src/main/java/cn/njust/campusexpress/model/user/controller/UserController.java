package cn.njust.campusexpress.model.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.VerifySceneEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.common.util.SessionUtil;
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

//用户模块Controller
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserAuditRecordService userAuditRecordService;
    private final UserBanRecordService userBanRecordService;
    private final VerifyCodeService verifyCodeService;

    //注册（手机号或邮箱已存在且密码正确时，为该账号追加一个新角色）
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<Void> register(@Valid @ModelAttribute UserRegisterDTO registerDTO,
                          @RequestPart(value = "material", required = false) MultipartFile material) {
        userService.register(registerDTO, material);
        return Result.success();
    }

    //登录
    @PostMapping("/login")
    Result<Void> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        Long userId = userService.login(loginDTO);
        StpUtil.login(userId);
        StpUtil.getTokenSession().set(SessionUtil.ROLE_KEY, loginDTO.getRole().name());
        return Result.success();
    }


    //登出账号
    @PostMapping("/logout")
    @SaCheckLogin
    Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    //首页探测登录状态，匿名或失效会话返回空数据
    @GetMapping("/session")
    Result<UserProfileVO> getSession() {
        if (!StpUtil.isLogin()) {
            return Result.success();
        }
        return Result.success(userService.getProfile(SessionUtil.userId(), SessionUtil.role()));
    }

    //获取账号信息
    @GetMapping("/profile")
    @SaCheckLogin
    Result<UserProfileVO> getProfile() {
        UserProfileVO profile = userService.getProfile(SessionUtil.userId(), SessionUtil.role());
        return Result.success(profile);
    }

    //更新用户名
    @PutMapping("/username")
    @SaCheckLogin
    Result<UserProfileVO> updateUsername(@Valid @RequestBody UserUsernameDTO dto) {
        UserProfileVO profile = userService.updateUsername(SessionUtil.userId(), SessionUtil.role(), dto.getUsername());
        return Result.success(profile);
    }

    //更新性别
    @PutMapping("/gender")
    @SaCheckLogin
    Result<UserProfileVO> updateGender(@Valid @RequestBody UserGenderDTO dto) {
        UserProfileVO profile = userService.updateGender(SessionUtil.userId(), SessionUtil.role(), dto.getGender());
        return Result.success(profile);
    }

    //更新头像
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckLogin
    Result<UserProfileVO> updateAvatar(@RequestParam("file") MultipartFile file) {
        UserProfileVO profile = userService.updateAvatar(SessionUtil.userId(), SessionUtil.role(), file);
        return Result.success(profile);
    }

    //修改密码
    @PutMapping("/password")
    @SaCheckLogin
    Result<Void> updatePassword(@Valid @RequestBody UserPasswordDTO dto) {
        userService.updatePassword(SessionUtil.userId(), dto);
        return Result.success();
    }

    //注销账号
    @DeleteMapping("/account")
    @SaCheckLogin
    Result<Void> deleteAccount() {
        userService.deleteAccount(SessionUtil.userId(), SessionUtil.role());
        return Result.success();
    }

    //发送验证码
    @PostMapping("/verify-code")
    Result<String> sendVerifyCode(@Valid @RequestBody SendCodeDTO dto) {
        if (dto.getScene() == VerifySceneEnum.FORGOT_PASSWORD) {
            userService.validatePasswordResetAccount(dto.getAccount());
        }
        String code = verifyCodeService.send(dto.getAccount(), dto.getScene());
        return Result.success(code);
    }

    //忘记密码第一步，校验验证码
    @PostMapping("/verify-code/check")
    Result<Void> checkVerifyCode(@Valid @RequestBody VerifyCodeDTO dto) {
        if (dto.getScene() != VerifySceneEnum.FORGOT_PASSWORD) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
        userService.validatePasswordResetAccount(dto.getAccount());
        verifyCodeService.validate(dto.getAccount(), dto.getScene(), dto.getCode());
        return Result.success();
    }

    //忘记密码第二部，重置密码
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
    @PutMapping("/audit/{recordId}")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> auditUser(@PathVariable Long recordId, @Valid @RequestBody UserAuditDTO dto) {
        userAuditRecordService.auditUser(recordId, dto);
        return Result.success();
    }

    //获取所有账号信息
    @GetMapping("/all-users")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<PageResult<UserProfileAdminVO>> getAllUsers(@Valid UserQueryDTO dto) {
        Page<UserProfileAdminVO> page = userService.getAllUsers(dto);
        return Result.success(PageResult.of(page));
    }

    //封禁账号
    @PostMapping("/{userId}/roles/{role}/ban")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> banUser(@PathVariable Long userId, @PathVariable UserRoleEnum role, @Valid @RequestBody UserBanDTO dto) {
        userBanRecordService.banUser(userId, role, dto);
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
    @PostMapping("/{userId}/roles/{role}/unban")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> unbanUser(@PathVariable Long userId, @PathVariable UserRoleEnum role) {
        userBanRecordService.unbanUser(userId, role);
        return Result.success();
    }

    //强制下线
    @PostMapping("/{userId}/kickout")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> kickoutUser(@PathVariable Long userId) {
        StpUtil.kickout(userId);
        return Result.success();
    }

    //管理员重置他人密码
    @PostMapping("/{userId}/reset-password")
    @SaCheckRole(UserRoleEnum.ROLE_ADMIN)
    Result<Void> adminResetPassword(@PathVariable Long userId, @Valid @RequestBody AdminResetPasswordDTO dto) {
        userService.adminResetPassword(userId, dto);
        return Result.success();
    }
}
