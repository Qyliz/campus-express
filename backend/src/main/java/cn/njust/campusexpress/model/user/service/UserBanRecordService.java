package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.model.user.dto.UserBanDTO;
import cn.njust.campusexpress.model.user.dto.UserBanQueryDTO;
import cn.njust.campusexpress.model.user.dto.UserUnbanDTO;
import cn.njust.campusexpress.model.user.entity.UserBanRecord;
import cn.njust.campusexpress.model.user.vo.UserBanRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;


public interface UserBanRecordService extends IRepository<UserBanRecord> {

    void banUser(UserBanDTO dto);

    void unbanUser(UserUnbanDTO dto);

    Page<UserBanRecordVO> getBanRecordPage(UserBanQueryDTO dto);
}
