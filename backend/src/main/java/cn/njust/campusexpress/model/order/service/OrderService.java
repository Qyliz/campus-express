package cn.njust.campusexpress.model.order.service;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.entity.DeliveryException;
import cn.njust.campusexpress.model.order.entity.ExpressOrder;
import cn.njust.campusexpress.model.order.vo.OrderDetailVO;

/** 订单业务接口，具体实现位于 service.impl。 */
public interface OrderService {
    Long reportException(Long userId, UserRoleEnum role, Long id, ReportExceptionDTO dto);
    PageResult<DeliveryException> listExceptions(Long userId, UserRoleEnum role, ExceptionQueryDTO dto);
    DeliveryException exceptionDetail(Long userId, UserRoleEnum role, Long id);
    void resolveException(Long userId, UserRoleEnum role, Long id, ResolveExceptionDTO dto);

    /** 创建待支付订单，返回订单 ID。 */
    Long create(Long userId, UserRoleEnum role, CreateOrderDTO dto);

    /** 根据 mine、available、assigned、admin 范围分页查询订单。 */
    PageResult<ExpressOrder> list(Long userId, UserRoleEnum role, String scope, OrderQueryDTO dto);

    /** 校验访问权限并查询订单详情、状态流转记录。 */
    OrderDetailVO detail(Long userId, UserRoleEnum role, Long id);

    /** 执行支付、接单、揽收、送达、确认取件或取消；取消时必须提供原因。 */
    void act(Long userId, UserRoleEnum role, Long id, String action, String reason);
}
