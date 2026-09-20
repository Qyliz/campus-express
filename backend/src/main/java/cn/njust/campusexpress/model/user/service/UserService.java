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

//用户模块Service
public interface UserService extends IRepository<User> {

    //注册新用户，或凭密码为已有用户追加角色
    void register(UserRegisterDTO registerDTO, MultipartFile material);

    //校验登录信息，返回用户ID
    Long login(UserLoginDTO loginDTO);

    //查询当前用户资料
    UserProfileVO getProfile(Long userId, UserRoleEnum role);

    //更新当前用户的用户名
    UserProfileVO updateUsername(Long userId, UserRoleEnum role, String username);

    //更新当前用户的性别
    UserProfileVO updateGender(Long userId, UserRoleEnum role, UserGenderEnum gender);

    //更新当前用户的头像
    UserProfileVO updateAvatar(Long userId, UserRoleEnum role, MultipartFile file);

    //校验旧密码并修改当前用户密码
    void updatePassword(Long userId, UserPasswordDTO dto);

    //注销当前角色账户
    void deleteAccount(Long userId, UserRoleEnum role);

    //校验找回密码账号
    void validatePasswordResetAccount(String account);

    //通过验证码重置密码
    void resetPassword(ResetPasswordDTO dto);

    //管理员重置指定用户密码
    void adminResetPassword(Long userId, AdminResetPasswordDTO dto);

    //换绑当前用户手机号
    void updatePhone(Long userId, ChangePhoneDTO dto);

    //换绑当前用户邮箱
    void updateEmail(Long userId, ChangeEmailDTO dto);

    //管理员分页查询用户及其角色账户
    Page<UserProfileAdminVO> getAllUsers(UserQueryDTO dto);
}
