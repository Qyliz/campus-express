package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.model.user.dto.AdminResetPasswordDTO;
import cn.njust.campusexpress.model.user.dto.ChangeEmailDTO;
import cn.njust.campusexpress.model.user.dto.ChangePhoneDTO;
import cn.njust.campusexpress.model.user.dto.ResetPasswordDTO;
import cn.njust.campusexpress.model.user.dto.UserLoginDTO;
import cn.njust.campusexpress.model.user.dto.UserPasswordDTO;
import cn.njust.campusexpress.model.user.dto.UserQueryDTO;
import cn.njust.campusexpress.model.user.dto.UserRegisterDTO;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.vo.UserProfileAdminVO;
import cn.njust.campusexpress.model.user.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IRepository<User> {
    void register(UserRegisterDTO registerDTO, MultipartFile material);

    Long login(UserLoginDTO loginDTO);

    UserProfileVO getProfile(Long userRoleId);

    UserProfileVO updateUsername(Long userRoleId, String username);

    UserProfileVO updateGender(Long userRoleId, UserGenderEnum gender);

    UserProfileVO updateAvatar(Long userRoleId, MultipartFile file);

    void updatePassword(Long userRoleId, UserPasswordDTO dto);

    void deleteAccount(Long userRoleId);

    void resetPassword(ResetPasswordDTO dto);

    void adminResetPassword(AdminResetPasswordDTO dto);

    void updatePhone(Long userRoleId, ChangePhoneDTO dto);

    void updateEmail(Long userRoleId, ChangeEmailDTO dto);

    Page<UserProfileAdminVO> getAllUsers(UserQueryDTO dto);
}
