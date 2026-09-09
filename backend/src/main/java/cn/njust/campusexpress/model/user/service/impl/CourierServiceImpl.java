package cn.njust.campusexpress.model.user.service.impl;

import cn.njust.campusexpress.model.user.entity.Courier;
import cn.njust.campusexpress.model.user.mapper.CourierMapper;
import cn.njust.campusexpress.model.user.service.CourierService;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class CourierServiceImpl extends CrudRepository<CourierMapper, Courier>
        implements CourierService {
}
