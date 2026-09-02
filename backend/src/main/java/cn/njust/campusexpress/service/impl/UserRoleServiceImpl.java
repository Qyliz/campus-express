package cn.njust.campusexpress.service.impl;

import cn.njust.campusexpress.entity.UserRole;
import cn.njust.campusexpress.mapper.UserRoleMapper;
import cn.njust.campusexpress.service.UserRoleService;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl extends CrudRepository<UserRoleMapper, UserRole>
        implements UserRoleService {

}




