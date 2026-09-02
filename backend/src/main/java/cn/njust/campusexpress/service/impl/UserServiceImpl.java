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
        String hashPassword = BCrypt.hashpw(registerDTO.getPassword(), BCrypt.gensalt());
        User user = new User();
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(hashPassword);
        save(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setUsername(registerDTO.getUsername());
        userRole.setRole(registerDTO.getRole());
        userRole.setGender(registerDTO.getGender());
        userRole.setStatus(UserStatusEnum.REVIEWING);
        userRoleService.save(userRole);
    }

    @Override
    public Long login(UserLoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        User user;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(User::getPassword);
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
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "账号格式错误");
        }
        user = getOne(wrapper);
        //账号不存在
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //密码错误
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_ERROR);
        }
        return user.getId();
    }
}

