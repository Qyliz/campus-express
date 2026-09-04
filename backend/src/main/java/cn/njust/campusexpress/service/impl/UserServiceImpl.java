package cn.njust.campusexpress.service.impl;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.dto.UserLoginDTO;
import cn.njust.campusexpress.dto.UserRegisterDTO;
import cn.njust.campusexpress.entity.User;
import cn.njust.campusexpress.entity.UserRole;
import cn.njust.campusexpress.mapper.UserMapper;
import cn.njust.campusexpress.service.UserRoleService;
import cn.njust.campusexpress.service.UserService;
import cn.njust.campusexpress.vo.UserProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends CrudRepository<UserMapper, User>
        implements UserService {

    private final UserRoleService userRoleService;

    //注册
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO registerDTO) {
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
        //收寄件人初始账号状态为正常，否则为审核中
        if (registerDTO.getRole() == UserRoleEnum.CUSTOMER) {
            userRole.setStatus(UserStatusEnum.NORMAL);
        } else {
            userRole.setStatus(UserStatusEnum.REVIEWING);
        }
        userRoleService.save(userRole);
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
        String newAvatar = saveAvatar(file);
        //更新数据库
        try {
            userRole.setAvatar(newAvatar);
            boolean success = userRoleService.updateById(userRole);
            if (!success) {
                throw new BusinessException(ResultCodeEnum.FILE_UPLOAD_ERROR);
            }
        } catch (Exception e) {
            deleteAvatar(newAvatar);
            throw e;
        }
        //删除旧头像
        deleteAvatar(oldAvatar);
        return getProfile(userRoleId);
    }

    //存储头像文件
    private String saveAvatar(MultipartFile file) {
        //检查文件类型
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.FILE_EMPTY);
        }
        String suffix;
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        switch (contentType) {
            case "image/jpeg" -> suffix = ".jpg";
            case "image/png" -> suffix = ".png";
            case "image/webp" -> suffix = ".webp";
            default ->
                    throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        String filename;
        Path path;
        do {
            filename = UUID.randomUUID() + suffix;
            path = Paths.get("upload/avatar", filename);
        } while (Files.exists(path));
        try {
            Files.createDirectories(path.getParent());
            file.transferTo(path);
            return "/upload/avatar/" + filename;
        } catch (IOException e) {
            throw new BusinessException(ResultCodeEnum.FILE_UPLOAD_ERROR);
        }
    }

    //删除本地头像文件
    private void deleteAvatar(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return;
        }
        Path path = Paths.get("." + avatar);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除头像失败：{}", avatar, e);
        }
    }
}

