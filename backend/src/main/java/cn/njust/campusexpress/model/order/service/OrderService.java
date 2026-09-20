package cn.njust.campusexpress.model.order.service;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.enums.OrderActionEnum;
import cn.njust.campusexpress.common.enums.OrderListScopeEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.entity.DeliveryException;
import cn.njust.campusexpress.model.order.entity.ExpressOrder;
import cn.njust.campusexpress.model.order.vo.OrderDetailVO;

//订单模块Service
public interface OrderService {
    //创建待支付订单，返回订单ID
    Long create(Long userId, UserRoleEnum role, CreateOrderDTO dto);

    //按当前身份和列表范围分页查询订单
    PageResult<ExpressOrder> list(Long userId, UserRoleEnum role, OrderListScopeEnum scope, OrderQueryDTO dto);

    //校验访问权限并查询订单详情、状态流转记录
    OrderDetailVO detail(Long userId, UserRoleEnum role, Long id);

    //执行支付、接单、揽收、送达、确认取件或取消
    void act(Long userId, UserRoleEnum role, Long id, OrderActionEnum action, String reason);

    //配送员上报异常
    Long reportException(Long userId, UserRoleEnum role, Long id, ReportExceptionDTO dto);

    //管理员查询异常列表
    PageResult<DeliveryException> listExceptions(Long userId, UserRoleEnum role, ExceptionQueryDTO dto);

    //订单参与者或管理员查看单条配送异常
    DeliveryException exceptionDetail(Long userId, UserRoleEnum role, Long id);

    //管理员处理异常
    void resolveException(Long userId, UserRoleEnum role, Long id, ResolveExceptionDTO dto);
}
