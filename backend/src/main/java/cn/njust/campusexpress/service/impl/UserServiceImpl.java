package cn.njust.campusexpress.service.impl;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends CrudRepository<UserMapper, User>
        implements UserService {

    private final UserRoleService userRoleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO registerDTO) {
        if (registerDTO.getRole() == UserRoleEnum.ADMIN) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "角色不能为管理员");
        }

        User user = new User();
        //验证手机号是否重复
        String phone = registerDTO.getPhone();
        if (phone != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(User::getPhone).eq(User::getPhone, phone);
            User temp = getOne(wrapper);
            if (temp != null) {
                throw new BusinessException(ResultCodeEnum.PHONE_ALREADY_BIND);
            }
            user.setPassword(phone);
        }
        //验证邮箱是否重复
        String email = registerDTO.getEmail();
        if (email != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(User::getEmail).eq(User::getEmail, email);
            User temp = getOne(wrapper);
            if (temp != null) {
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

    @Override
    public Long login(UserLoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        LambdaQueryWrapper<User> wrapper1 = new LambdaQueryWrapper<>();
        //邮箱登录
        if (account.contains("@") && account.length() < 255) {
            wrapper1.eq(User::getEmail, account);
        }
        //手机号登录
        else if (account.matches("^1[3-9]\\d{9}$")) {
            wrapper1.eq(User::getPhone, account);
        }
        //账号格式错误
        else {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "账号格式错误");
        }
        //获取账号信息
        wrapper1.select(User::getId, User::getPassword);
        User user = getOne(wrapper1);
        //账号不存在
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //获取账号状态
        Long userId = user.getId();
        LambdaQueryWrapper<UserRole> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.select(UserRole::getStatus).eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleService.getOne(wrapper2);
        //处理异常账号状态
        switch (userRole.getStatus()) {
            case REVIEWING ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_REVIEWING);
            case REJECTED ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_REJECTED);
            case DISABLED ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        //密码错误
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_ERROR);
        }
        return user.getId();
    }
}

