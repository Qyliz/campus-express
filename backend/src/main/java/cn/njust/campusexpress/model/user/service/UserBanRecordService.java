package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.model.user.dto.UserBanDTO;
import cn.njust.campusexpress.model.user.dto.UserBanQueryDTO;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.model.user.entity.UserBanRecord;
import cn.njust.campusexpress.model.user.vo.UserBanRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;

//角色账户封禁模块Service
public interface UserBanRecordService extends IRepository<UserBanRecord> {

    //封禁指定角色账户
    void banUser(Long userId, UserRoleEnum role, UserBanDTO dto);

    //解封指定角色账户
    void unbanUser(Long userId, UserRoleEnum role);

    //分页查询角色账户封禁记录
    Page<UserBanRecordVO> getBanRecordPage(UserBanQueryDTO dto);
}
