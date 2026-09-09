package cn.njust.campusexpress.model.user.service.impl;

import cn.njust.campusexpress.model.user.entity.Admin;
import cn.njust.campusexpress.model.user.mapper.AdminMapper;
import cn.njust.campusexpress.model.user.service.AdminService;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl extends CrudRepository<AdminMapper, Admin>
        implements AdminService {
}
