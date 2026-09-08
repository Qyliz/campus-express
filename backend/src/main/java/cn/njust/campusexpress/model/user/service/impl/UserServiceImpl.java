package cn.njust.campusexpress.model.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.common.util.FileUtil;
import cn.njust.campusexpress.model.user.dto.*;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.entity.UserAuditRecord;
import cn.njust.campusexpress.model.user.entity.UserRole;
import cn.njust.campusexpress.model.user.mapper.UserMapper;
import cn.njust.campusexpress.model.user.service.UserAuditRecordService;
import cn.njust.campusexpress.model.user.service.UserRoleService;
import cn.njust.campusexpress.model.user.service.UserService;
import cn.njust.campusexpress.model.user.service.VerifyCodeService;
import cn.njust.campusexpress.model.user.vo.UserProfileAdminVO;
import cn.njust.campusexpress.model.user.vo.UserProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@SuppressWarnings("DuplicatedCode")
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends CrudRepository<UserMapper, User>
        implements UserService {

    private final UserRoleService userRoleService;
    private final UserAuditRecordService userAuditRecordService;
    private final UserMapper userMapper;
    private final VerifyCodeService verifyCodeService;

    //注册
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO registerDTO, MultipartFile material) {
        if (registerDTO.getRole() == UserRoleEnum.ADMIN) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "角色不能为管理员");
        }

        String phone = registerDTO.getPhone();
        String email = registerDTO.getEmail();
        //未输入手机号和邮箱
        if (phone == null && email == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_MISSING, "手机号和邮箱请至少输入一项");
        }
        User user = new User();
        //验证手机号是否重复
        if (phone != null) {
            boolean exist = lambdaQuery().eq(User::getPhone, phone).exists();
            if (exist) {
                throw new BusinessException(ResultCodeEnum.PHONE_ALREADY_BIND);
            }
            user.setPhone(phone);
        }
        //验证邮箱是否重复
        if (email != null) {
            boolean exist = lambdaQuery().eq(User::getEmail, email).exists();
            if (exist) {
                throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_BIND);
            }
            user.setEmail(email);
        }

        //加密密码
        String hashPassword = BCrypt.hashpw(registerDTO.getPassword(), BCrypt.gensalt());
        user.setPassword(hashPassword);
        //存入user表
        save(user);
        //存入user_role表
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setUsername(registerDTO.getUsername());
        userRole.setRole(registerDTO.getRole());
        userRole.setGender(registerDTO.getGender());

        //角色为收寄件人，初始账号状态为正常，否则为审核中
        if (registerDTO.getRole() == UserRoleEnum.CUSTOMER) {
            userRole.setStatus(UserStatusEnum.NORMAL);
        } else {
            userRole.setStatus(UserStatusEnum.REVIEWING);
        }
        userRoleService.save(userRole);

        //仅为需要审核的角色（配送员）建立审核记录；收寄件人状态为正常，无需审核
        if (registerDTO.getRole() != UserRoleEnum.CUSTOMER) {
            //审核角色必须提交材料图片；缺失/为空由 FileUtil 抛 FILE_EMPTY
            String materialPath = FileUtil.saveImage("upload/audit", material);
            UserAuditRecord record = new UserAuditRecord();
            record.setUserRoleId(userRole.getId());
            record.setMaterial(materialPath);
            userAuditRecordService.save(record);
        }
    }

    //登录
    @Override
    public Long login(UserLoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        //邮箱登录
        if (account.contains("@") && account.length() < 255) {
            wrapper.eq(User::getEmail, account);
        }
        //手机号登录
        else if (account.matches("^1[3-9]\\d{9}$")) {
            wrapper.eq(User::getPhone, account);
        }
        //账号格式错误
        else {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请输入正确的邮箱或手机号");
        }
        //获取账号信息
        wrapper.select(User::getId, User::getPassword);
        User user = getOne(wrapper);
        //账号不存在
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR);
        }
        //获取账号id和状态
        Long userId = user.getId();
        UserRole userRole = userRoleService.lambdaQuery()
                .select(UserRole::getId, UserRole::getStatus)
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, loginDTO.getRole())
                .one();
        //账号没有该角色
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR);
        }
        //密码错误
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR);
        }
        //处理异常账号状态
        switch (userRole.getStatus()) {
            case REVIEWING ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_REVIEWING);
            case REJECTED ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_REJECTED);
            case DISABLED ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        return userRole.getId();
    }

    //获取账号资料
    @Override
    public UserProfileVO getProfile(Long userRoleId) {
        UserRole userRole = userRoleService.getById(userRoleId);
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        User user = getById(userRole.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        UserProfileVO profile = new UserProfileVO();
        profile.setUsername(userRole.getUsername());
        profile.setRole(userRole.getRole());
        profile.setGender(userRole.getGender());
        profile.setPhone(user.getPhone());
        profile.setEmail(user.getEmail());
        profile.setAvatar(userRole.getAvatar());
        return profile;
    }

    //更新用户名
    @Override
    public UserProfileVO updateUsername(Long userRoleId, String username) {
        UserRole userRole = userRoleService.getById(userRoleId);
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        userRole.setUsername(username);
        userRoleService.updateById(userRole);
        return getProfile(userRoleId);
    }

    //更新性别
    @Override
    public UserProfileVO updateGender(Long userRoleId, UserGenderEnum gender) {
        UserRole userRole = userRoleService.getById(userRoleId);
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        userRole.setGender(gender);
        userRoleService.updateById(userRole);
        return getProfile(userRoleId);
    }

    //更新头像
    @Override
    public UserProfileVO updateAvatar(Long userRoleId, MultipartFile file) {
        //获取账号数据
        UserRole userRole = userRoleService.getById(userRoleId);
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //获取旧头像
        String oldAvatar = userRole.getAvatar();
        //存储新头像文件
        String newAvatar = FileUtil.saveImage("upload/avatar", file);
        //更新数据库
        try {
            userRole.setAvatar(newAvatar);
            boolean success = userRoleService.updateById(userRole);
            if (!success) {
                throw new BusinessException(ResultCodeEnum.FILE_UPLOAD_ERROR);
            }
        } catch (Exception e) {
            FileUtil.deleteImage(newAvatar);
            throw e;
        }
        //删除旧头像
        FileUtil.deleteImage(oldAvatar);
        return getProfile(userRoleId);
    }

    //修改密码
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userRoleId, UserPasswordDTO dto) {
        UserRole userRole = userRoleService.getById(userRoleId);
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        User user = getById(userRole.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //校验旧密码
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.OLD_PASSWORD_ERROR);
        }
        //写入新密码
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        updateById(user);
        //修改成功后退出该账号会话，强制用新密码重新登录
        StpUtil.logout(userRoleId);
    }

    //注销账号（仅逻辑删除当前角色账号）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userRoleId) {
        UserRole userRole = userRoleService.getById(userRoleId);
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //仅逻辑删除当前 user_role 行（@TableLogic 置 deleted=id），user 主表保留
        userRoleService.removeById(userRoleId);
        //注销后退出该账号会话
        StpUtil.logout(userRoleId);
    }

    //忘记密码：校验验证码 -> 重置密码 -> 踢出该账号所有角色会话
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO dto) {
        verifyCodeService.verify(dto.getAccount(), VerifySceneEnum.FORGOT_PASSWORD, dto.getCode());
        User user = resolveUserByAccount(dto.getAccount());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        updateById(user);
        //改密后踢出该用户名下所有角色的在线会话，强制用新密码重新登录
        kickoutAllRoles(user.getId());
    }

    //管理员重置他人密码：改密 -> 踢出该账号所有角色会话
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminResetPassword(AdminResetPasswordDTO dto) {
        User user = currentUser(dto.getUserRoleId());
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        updateById(user);
        kickoutAllRoles(user.getId());
    }

    //换绑手机号（登录态）：校验验证码 -> 唯一性 -> 更新
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePhone(Long userRoleId, ChangePhoneDTO dto) {
        User user = currentUser(userRoleId);
        verifyCodeService.verify(dto.getNewPhone(), VerifySceneEnum.CHANGE_PHONE, dto.getCode());
        if (lambdaQuery().eq(User::getPhone, dto.getNewPhone()).ne(User::getId, user.getId()).exists()) {
            throw new BusinessException(ResultCodeEnum.PHONE_ALREADY_BIND);
        }
        user.setPhone(dto.getNewPhone());
        updateById(user);
    }

    //换绑邮箱（登录态）：校验验证码 -> 唯一性 -> 更新
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(Long userRoleId, ChangeEmailDTO dto) {
        User user = currentUser(userRoleId);
        verifyCodeService.verify(dto.getNewEmail(), VerifySceneEnum.CHANGE_EMAIL, dto.getCode());
        if (lambdaQuery().eq(User::getEmail, dto.getNewEmail()).ne(User::getId, user.getId()).exists()) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_BIND);
        }
        user.setEmail(dto.getNewEmail());
        updateById(user);
    }

    //获取当前登录角色对应的 user
    private User currentUser(Long userRoleId) {
        UserRole userRole = userRoleService.getById(userRoleId);
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        User user = getById(userRole.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        return user;
    }

    //按手机号或邮箱解析 user（格式非法抛 PARAM_ERROR，未找到返回 null）
    private User resolveUserByAccount(String account) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (account.contains("@") && account.length() < 255) {
            wrapper.eq(User::getEmail, account);
        } else if (account.matches("^1[3-9]\\d{9}$")) {
            wrapper.eq(User::getPhone, account);
        } else {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请输入正确的邮箱或手机号");
        }
        return getOne(wrapper);
    }

    //踢出某用户名下所有角色的在线会话（密码存于 user 主表，为所有角色共享）
    private void kickoutAllRoles(Long userId) {
        List<UserRole> roles = userRoleService.lambdaQuery().eq(UserRole::getUserId, userId).list();
        for (UserRole role : roles) {
            StpUtil.kickout(role.getId());
        }
    }

    //获取所有账号信息
    @Override
    public Page<UserProfileAdminVO> getAllUsers(UserQueryDTO dto) {
        //页码为空时默认为第1页
        int currentPage = dto.getCurrentPage() == null ? 1 : dto.getCurrentPage();
        Page<UserProfileAdminVO> page = new Page<>(currentPage, 10);
        //关联 user 与 user_role 表分页查询，查询条件与排序在 UserMapper.xml 中动态拼接
        userMapper.selectUserPage(page, dto);
        return page;
    }
}

