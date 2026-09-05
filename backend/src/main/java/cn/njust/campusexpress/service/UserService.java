package cn.njust.campusexpress.service;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.dto.UserLoginDTO;
import cn.njust.campusexpress.dto.UserQueryDTO;
import cn.njust.campusexpress.dto.UserRegisterDTO;
import cn.njust.campusexpress.entity.User;
import cn.njust.campusexpress.vo.UserProfileAdminVO;
import cn.njust.campusexpress.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IRepository<User> {
    void register(UserRegisterDTO registerDTO);

    Long login(@Valid UserLoginDTO loginDTO);

    UserProfileVO getProfile(Long userRoleId);

    UserProfileVO updateUsername(Long userRoleId, @NotBlank String username);

    UserProfileVO updateGender(Long userRoleId, @NotBlank UserGenderEnum gender);

    UserProfileVO updateAvatar(Long userRoleId, MultipartFile file);

    Page<UserProfileAdminVO> getAllUsers(UserQueryDTO dto);
}
