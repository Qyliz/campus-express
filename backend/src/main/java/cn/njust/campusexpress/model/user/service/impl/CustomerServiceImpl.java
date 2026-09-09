package cn.njust.campusexpress.model.user.service.impl;

import cn.njust.campusexpress.model.user.entity.Customer;
import cn.njust.campusexpress.model.user.mapper.CustomerMapper;
import cn.njust.campusexpress.model.user.service.CustomerService;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceImpl extends CrudRepository<CustomerMapper, Customer>
        implements CustomerService {
}
