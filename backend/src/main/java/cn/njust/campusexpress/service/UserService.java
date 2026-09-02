package cn.njust.campusexpress.service;

import cn.njust.campusexpress.dto.UserLoginDTO;
import cn.njust.campusexpress.dto.UserRegisterDTO;
import cn.njust.campusexpress.entity.User;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import jakarta.validation.Valid;

public interface UserService extends IRepository<User> {
    void register(UserRegisterDTO registerDTO);

    Long login(@Valid UserLoginDTO loginDTO);
}
