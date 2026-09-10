package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
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

    /**
     * 注册。手机号或邮箱已属于某个账号且密码正确时，视为本人追加一个新角色；
     * 追加时不会覆盖已有的用户名、性别、头像。
     */
    void register(UserRegisterDTO registerDTO, MultipartFile material);

    /**
     * 登录校验
     *
     * @return user 表主键，直接用作 Sa-Token 的 loginId
     */
    Long login(UserLoginDTO loginDTO);

    UserProfileVO getProfile(Long userId, UserRoleEnum role);

    UserProfileVO updateUsername(Long userId, UserRoleEnum role, String username);

    UserProfileVO updateGender(Long userId, UserRoleEnum role, UserGenderEnum gender);

    UserProfileVO updateAvatar(Long userId, UserRoleEnum role, MultipartFile file);

    void updatePassword(Long userId, UserPasswordDTO dto);

    /**
     * 注销：只逻辑删除当前角色的账户行，user 主表与其他角色账户保留
     */
    void deleteAccount(Long userId, UserRoleEnum role);

    void resetPassword(ResetPasswordDTO dto);

    void adminResetPassword(Long userId, AdminResetPasswordDTO dto);

    void updatePhone(Long userId, ChangePhoneDTO dto);

    void updateEmail(Long userId, ChangeEmailDTO dto);

    Page<UserProfileAdminVO> getAllUsers(UserQueryDTO dto);
}
