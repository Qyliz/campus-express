package cn.njust.campusexpress.model.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.common.util.FileUtil;
import cn.njust.campusexpress.model.user.dto.*;
import cn.njust.campusexpress.model.user.entity.RoleAccount;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.entity.UserAuditRecord;
import cn.njust.campusexpress.model.user.mapper.UserMapper;
import cn.njust.campusexpress.model.user.service.RoleAccountService;
import cn.njust.campusexpress.model.user.service.UserAuditRecordService;
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

@SuppressWarnings("DuplicatedCode")
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends CrudRepository<UserMapper, User>
        implements UserService {

    private final RoleAccountService roleAccountService;
    private final UserAuditRecordService userAuditRecordService;
    private final UserMapper userMapper;
    private final VerifyCodeService verifyCodeService;

    //注册：新账号，或凭密码给已有账号追加一个新角色
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

        //回查已有账号（逻辑删除的行查不到）。命中同一个 user 说明是本人追加角色，命中两个不同 user 则是参数冲突
        User byPhone = phone == null ? null : lambdaQuery().eq(User::getPhone, phone).one();
        User byEmail = email == null ? null : lambdaQuery().eq(User::getEmail, email).one();
        if (byPhone != null && byEmail != null && !byPhone.getId().equals(byEmail.getId())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "手机号和邮箱属于不同账号");
        }

        User existing = byPhone != null ? byPhone : byEmail;
        User user;
        if (existing == null) {
            //新账号：用户名、性别、头像属于「人」的共有资料，与凭证一起存在 user 主表
            user = new User();
            user.setUsername(registerDTO.getUsername());
            user.setGender(registerDTO.getGender());
            user.setPhone(phone);
            user.setEmail(email);
            user.setPassword(BCrypt.hashpw(registerDTO.getPassword(), BCrypt.gensalt()));
            save(user);
        } else {
            //追加角色：必须凭正确密码证明是本人；密码不符时沿用「已被注册」提示，不额外泄露账号是否存在
            if (!BCrypt.checkpw(registerDTO.getPassword(), existing.getPassword())) {
                throw new BusinessException(byPhone != null
                        ? ResultCodeEnum.PHONE_ALREADY_BIND
                        : ResultCodeEnum.EMAIL_ALREADY_BIND);
            }
            if (roleAccountService.getByUserAndRole(existing.getId(), registerDTO.getRole()) != null) {
                throw new BusinessException(ResultCodeEnum.ROLE_ALREADY_REGISTERED);
            }
            //已有资料不覆盖：第二次注册填的用户名/性别一律忽略
            user = existing;
        }

        //收寄件人注册后即可用，配送员要等管理员审核
        UserStatusEnum status = registerDTO.getRole() == UserRoleEnum.CUSTOMER
                ? UserStatusEnum.NORMAL
                : UserStatusEnum.REVIEWING;
        RoleAccount account = roleAccountService.createAccount(user.getId(), registerDTO.getRole(), status);

        //仅为配送员建立审核记录；审核材料必传，缺失/为空由 FileUtil 抛 FILE_EMPTY
        if (registerDTO.getRole() == UserRoleEnum.COURIER) {
            String materialPath = FileUtil.saveImage("upload/audit", material);
            UserAuditRecord record = new UserAuditRecord();
            record.setCourierId(account.getId());
            record.setMaterial(materialPath);
            //status 不显式设置，交由数据库默认值 2（审核中）
            userAuditRecordService.save(record);
        }
    }

    /**
     * 登录。返回 user.id 作为 Sa-Token 的 loginId，角色名由 controller 写入 token session。
     * <p>取舍：loginId 不带角色，且 sa-token.is-concurrent=false，所以一个人同一时刻只有一个在线会话，
     * 切换角色等于重新登录（旧 token 变为 BE_REPLACED）；同理踢下线也只能按用户整体踢，
     * 封禁其任一角色都会让其当前会话失效。换来的是登录态各处直接用 userId，无需在三张角色表间分发。</p>
     */
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
        //该用户是否持有请求的角色账户
        RoleAccount roleAccount = roleAccountService.getByUserAndRole(user.getId(), loginDTO.getRole());
        //账号没有该角色
        if (roleAccount == null) {
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR);
        }
        //密码错误
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR);
        }
        //处理异常账号状态
        switch (roleAccount.getStatus()) {
            case REVIEWING ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_REVIEWING);
            case REJECTED ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_REJECTED);
            case DISABLED ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        return user.getId();
    }

    //获取账号资料
    @Override
    public UserProfileVO getProfile(Long userId, UserRoleEnum role) {
        return toProfile(requireUser(userId), role);
    }

    //更新用户名
    @Override
    public UserProfileVO updateUsername(Long userId, UserRoleEnum role, String username) {
        User user = requireUser(userId);
        user.setUsername(username);
        updateById(user);
        return toProfile(user, role);
    }

    //更新性别
    @Override
    public UserProfileVO updateGender(Long userId, UserRoleEnum role, UserGenderEnum gender) {
        User user = requireUser(userId);
        user.setGender(gender);
        updateById(user);
        return toProfile(user, role);
    }

    //更新头像
    @Override
    public UserProfileVO updateAvatar(Long userId, UserRoleEnum role, MultipartFile file) {
        //获取账号数据
        User user = requireUser(userId);
        //获取旧头像
        String oldAvatar = user.getAvatar();
        //存储新头像文件
        String newAvatar = FileUtil.saveImage("upload/avatar", file);
        //更新数据库
        try {
            user.setAvatar(newAvatar);
            boolean success = updateById(user);
            if (!success) {
                throw new BusinessException(ResultCodeEnum.FILE_UPLOAD_ERROR);
            }
        } catch (Exception e) {
            FileUtil.deleteImage(newAvatar);
            throw e;
        }
        //删除旧头像
        FileUtil.deleteImage(oldAvatar);
        return toProfile(user, role);
    }

    //修改密码
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UserPasswordDTO dto) {
        User user = requireUser(userId);
        //校验旧密码
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.OLD_PASSWORD_ERROR);
        }
        //写入新密码
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        updateById(user);
        //修改成功后退出当前会话，强制用新密码重新登录
        StpUtil.logout();
    }

    //注销账号（仅逻辑删除当前角色账户）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, UserRoleEnum role) {
        //只软删当前角色那一行（@TableLogic 置 deleted=id），user 主表与其他角色账户保留
        boolean removed = roleAccountService.deleteByUserAndRole(userId, role);
        if (!removed) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //注销后退出当前会话
        StpUtil.logout();
    }

    //忘记密码：校验验证码 -> 重置密码 -> 踢出在线会话
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
        //密码为该用户名下所有角色共享，改密后踢下线强制重新登录
        StpUtil.kickout(user.getId());
    }

    //管理员重置他人密码：改密 -> 踢出在线会话
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminResetPassword(AdminResetPasswordDTO dto) {
        User user = requireUser(dto.getUserId());
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        updateById(user);
        StpUtil.kickout(user.getId());
    }

    //换绑手机号（登录态）：校验验证码 -> 唯一性 -> 更新
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePhone(Long userId, ChangePhoneDTO dto) {
        User user = requireUser(userId);
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
    public void updateEmail(Long userId, ChangeEmailDTO dto) {
        User user = requireUser(userId);
        verifyCodeService.verify(dto.getNewEmail(), VerifySceneEnum.CHANGE_EMAIL, dto.getCode());
        if (lambdaQuery().eq(User::getEmail, dto.getNewEmail()).ne(User::getId, user.getId()).exists()) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_BIND);
        }
        user.setEmail(dto.getNewEmail());
        updateById(user);
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

    //获取当前登录用户，不存在则抛 USER_NOT_FOUND
    private User requireUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        return user;
    }

    //资料全部来自 user 主表，role 只用于回显当前登录身份
    private UserProfileVO toProfile(User user, UserRoleEnum role) {
        UserProfileVO profile = new UserProfileVO();
        profile.setUsername(user.getUsername());
        profile.setRole(role);
        profile.setGender(user.getGender());
        profile.setPhone(user.getPhone());
        profile.setEmail(user.getEmail());
        profile.setAvatar(user.getAvatar());
        return profile;
    }

    //获取所有账号信息
    @Override
    public Page<UserProfileAdminVO> getAllUsers(UserQueryDTO dto) {
        //页码为空时默认为第1页
        int currentPage = dto.getCurrentPage() == null ? 1 : dto.getCurrentPage();
        Page<UserProfileAdminVO> page = new Page<>(currentPage, 10);
        //一人可持多个角色，故结果一行一个 (用户, 角色账户)；查询条件与排序在 UserMapper.xml 中动态拼接
        userMapper.selectUserPage(page, dto);
        return page;
    }
}
