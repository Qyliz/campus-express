package cn.njust.campusexpress.service.impl;

import cn.njust.campusexpress.common.Result;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.dto.UserLoginDTO;
import cn.njust.campusexpress.dto.UserRegisterDTO;
import cn.njust.campusexpress.entity.User;
import cn.njust.campusexpress.entity.UserRole;
import cn.njust.campusexpress.mapper.UserMapper;
import cn.njust.campusexpress.service.UserRoleService;
import cn.njust.campusexpress.service.UserService;
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
    public Result<Void> register(UserRegisterDTO registerDTO) {
        if (registerDTO.getRole() == UserRoleEnum.ADMIN) {
            return Result.error(400, "角色不能为管理员");
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
        
        return Result.success();
    }

    @Override
    public Result<Void> login(UserLoginDTO loginDTO) {
        return null;
    }
}

