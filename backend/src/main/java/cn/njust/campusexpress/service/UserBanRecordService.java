package cn.njust.campusexpress.service;

import cn.njust.campusexpress.dto.UserBanDTO;
import cn.njust.campusexpress.entity.UserBanRecord;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import jakarta.validation.Valid;


public interface UserBanRecordService extends IRepository<UserBanRecord> {

    void banUser(@Valid UserBanDTO dto);
}
