package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.model.user.dto.UserBanDTO;
import cn.njust.campusexpress.model.user.dto.UserBanQueryDTO;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.model.user.entity.UserBanRecord;
import cn.njust.campusexpress.model.user.vo.UserBanRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;


public interface UserBanRecordService extends IRepository<UserBanRecord> {

    void banUser(Long userId, UserRoleEnum role, UserBanDTO dto);

    void unbanUser(Long userId, UserRoleEnum role);

    Page<UserBanRecordVO> getBanRecordPage(UserBanQueryDTO dto);
}
