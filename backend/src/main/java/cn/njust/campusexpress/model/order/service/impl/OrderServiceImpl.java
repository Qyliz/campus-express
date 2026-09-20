package cn.njust.campusexpress.model.order.service.impl;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.entity.DeliveryException;
import cn.njust.campusexpress.model.order.entity.ExpressOrder;
import cn.njust.campusexpress.model.order.entity.OrderStatusRecord;
import cn.njust.campusexpress.model.order.mapper.DeliveryExceptionMapper;
import cn.njust.campusexpress.model.order.mapper.ExpressOrderMapper;
import cn.njust.campusexpress.model.order.mapper.OrderStatusRecordMapper;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.order.vo.OrderDetailVO;
import cn.njust.campusexpress.model.user.entity.Courier;
import cn.njust.campusexpress.model.user.entity.RoleAccount;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.mapper.CourierMapper;
import cn.njust.campusexpress.model.user.mapper.UserMapper;
import cn.njust.campusexpress.model.user.service.AccountGuard;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static cn.njust.campusexpress.common.enums.OrderStatusEnum.*;
import static cn.njust.campusexpress.common.enums.PaymentStatusEnum.PAID;
import static cn.njust.campusexpress.common.enums.PaymentStatusEnum.REFUNDED;
import static cn.njust.campusexpress.common.exception.BusinessException.invalid;

//订单模块ServiceImpl
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final ExpressOrderMapper orders;
    private final OrderStatusRecordMapper records;
    private final AccountGuard guard;
    private final UserMapper users;
    private final CourierMapper couriers;
    private final DeliveryExceptionMapper exceptions;

    //创建待支付订单，返回订单ID
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, UserRoleEnum role, CreateOrderDTO dto) {
        RoleAccount customer = guard.requireRole(userId, role, UserRoleEnum.CUSTOMER);
        //初始化订单的所属账户、订单状态和支付状态
        ExpressOrder order = new ExpressOrder();
        BeanUtils.copyProperties(dto, order);
        order.setCustomerId(customer.getId());
        order.setOrderStatus(UNPAID);
        order.setPaymentStatus(PaymentStatusEnum.UNPAID);
        order.setVersion(0);
        //保存订单和创建记录
        if (orders.insert(order) != 1) throw invalid("创建订单失败");
        record(order, null, userId, role, "创建订单");
        return order.getId();
    }

    //按当前身份和列表范围分页查询订单
    @Override
    public PageResult<ExpressOrder> list(Long userId, UserRoleEnum role, OrderListScopeEnum scope, OrderQueryDTO dto) {
        LambdaQueryWrapper<ExpressOrder> query = new LambdaQueryWrapper<>();
        //MINE范围查出的下单人账户id与user行，赋值给后面的局部变量，由Lambda表达式使用
        Long customerId = null;
        User user = null;
        switch (scope) {
            //与当前收寄件人有关的订单
            case MINE -> {
                Long mineCustomerId = guard.requireRole(userId, role, UserRoleEnum.CUSTOMER).getId();
                User mineUser = users.selectById(userId);
                customerId = mineCustomerId;
                user = mineUser;
                switch (dto.getRelation()) {
                    //当前收寄件人发布的订单
                    case CREATED ->
                            query.eq(ExpressOrder::getCustomerId, customerId);
                    //当前收寄件人收到的订单
                    case RECEIVED ->
                            query.eq(ExpressOrder::getDeliveryPhone, mineUser.getPhone());
                    //所有订单
                    case ALL ->
                            query.and(q -> q.eq(ExpressOrder::getCustomerId, mineCustomerId)
                                    .or().eq(ExpressOrder::getDeliveryPhone, mineUser.getPhone()));
                }
            }
            //当前配送员接取的订单
            case ASSIGNED -> query.eq(ExpressOrder::getCourierId,
                    guard.requireRole(userId, role, UserRoleEnum.COURIER).getId());
            //接单大厅中的待接订单
            case AVAILABLE -> {
                guard.requireRole(userId, role, UserRoleEnum.COURIER);
                query.eq(ExpressOrder::getOrderStatus, AVAILABLE);
                //排除已经注销的本人收寄件人账户发布的订单
                query.apply("customer_id not in (select id from customer where user_id = {0})", userId);
            }
            //管理端全部订单
            case ADMIN -> guard.requireRole(userId, role, UserRoleEnum.ADMIN);
        }
        //按订单状态筛选
        query.eq(dto.getOrderStatus() != null, ExpressOrder::getOrderStatus, dto.getOrderStatus());
        //管理员端按订单ID筛选
        if (scope == OrderListScopeEnum.ADMIN)
            query.eq(dto.getOrderId() != null, ExpressOrder::getId, dto.getOrderId());
        //按创建时间倒序，再按ID倒序
        query.orderByDesc(ExpressOrder::getCreateTime).orderByDesc(ExpressOrder::getId);
        //分页查询
        Page<ExpressOrder> page = orders.selectPage(PageResult.pageOf(dto.getCurrentPage()), query);
        //补充订单的信息
        if (!page.getRecords().isEmpty() && scope != OrderListScopeEnum.AVAILABLE) {
            //提取所有订单ID
            List<Long> ids = page.getRecords().stream().map(ExpressOrder::getId).toList();
            //补充订单信息：是否存在待处理异常
            Set<Long> pendingIds = exceptions.selectList(new LambdaQueryWrapper<DeliveryException>()
                            .in(DeliveryException::getOrderId, ids).eq(DeliveryException::getStatus, ExceptionStatusEnum.PENDING))
                    .stream().map(DeliveryException::getOrderId).collect(Collectors.toSet());
            page.getRecords().forEach(o -> o.setPendingException(pendingIds.contains(o.getId())));
            //补充订单信息：配送员姓名和手机号
            attachCouriers(page.getRecords());
            //如果角色是收寄件人，则补充订单与该用户的关系
            if (role == UserRoleEnum.CUSTOMER) {
                Long viewerCustomerId = customerId;
                User viewer = user;
                page.getRecords().forEach(o -> {
                    o.setCreatedByMe(Objects.equals(viewerCustomerId, o.getCustomerId()));
                    o.setReceivedByMe(recipient(o, viewer));
                });
            }
        }
        //如果是接单大厅的订单，则隐藏收寄件人的敏感信息
        if (scope == OrderListScopeEnum.AVAILABLE)
            page.getRecords().forEach(this::hideContacts);
        return PageResult.of(page);
    }

    //校验访问权限并查询订单详情、状态流转记录
    @Override
    public OrderDetailVO detail(Long userId, UserRoleEnum role, Long id) {
        RoleAccount account = guard.requireActiveAccount(userId, role);
        ExpressOrder order = get(id);
        User user = role == UserRoleEnum.CUSTOMER ? users.selectById(userId) : null;
        //校验访问权限
        if (role != UserRoleEnum.ADMIN) {
            participant(order, account, role, user);
        }
        //补充配送员信息
        attachCouriers(List.of(order));
        //补充订单的异常信息
        List<DeliveryException> history = history(id);
        order.setPendingException(history.stream().anyMatch(e -> e.getStatus() == ExceptionStatusEnum.PENDING));
        //初始化前端页面可执行的操作
        List<String> actions = new ArrayList<>();
        OrderStatusEnum state = order.getOrderStatus();
        boolean canReport = false;
        //设置管理员的操作
        if (role == UserRoleEnum.ADMIN && state != COMPLETED && state != CANCELLED)
            actions.add(OrderActionEnum.ADMIN_CANCEL.getCode());
        //设置收寄件人的操作
        if (role == UserRoleEnum.CUSTOMER) {
            order.setCreatedByMe(Objects.equals(account.getId(), order.getCustomerId()));
            order.setReceivedByMe(recipient(order, user));
            if (order.isCreatedByMe() && state == UNPAID)
                actions.add(OrderActionEnum.PAY.getCode());
            if (order.isCreatedByMe() && (state == UNPAID || state == AVAILABLE))
                actions.add(OrderActionEnum.CANCEL.getCode());
            if (state == AWAITING_COLLECTION)
                actions.add(OrderActionEnum.COMPLETE.getCode());
        }
        //设置配送员的操作
        if (role == UserRoleEnum.COURIER && !order.isPendingException()
                && (state == AWAITING_PICKUP || state == DELIVERING)) {
            canReport = true;
            actions.add(state == AWAITING_PICKUP ? OrderActionEnum.PICKUP.getCode() : OrderActionEnum.DELIVER.getCode());
        }
        //返回订单的详细信息
        OrderDetailVO detail = new OrderDetailVO();
        detail.setOrder(order);
        detail.setRecords(records.selectList(new LambdaQueryWrapper<OrderStatusRecord>()
                .eq(OrderStatusRecord::getOrderId, id)
                .orderByAsc(OrderStatusRecord::getCreateTime).orderByAsc(OrderStatusRecord::getId)));
        detail.setExceptions(history);
        detail.setAllowedActions(actions);
        detail.setCanReportException(canReport);
        return detail;
    }

    //执行支付、接单、揽收、送达、确认取件或取消
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void act(Long userId, UserRoleEnum role, Long id, OrderActionEnum action, String reason) {
        ExpressOrder order = get(id);
        OrderStatusEnum previous = order.getOrderStatus();
        String description;
        switch (action) {
            //收寄件人操作
            case PAY, COMPLETE, CANCEL -> {
                //校验角色是否为收寄件人
                RoleAccount customer = guard.requireRole(userId, role, UserRoleEnum.CUSTOMER);
                //根据操作进一步校验角色与订单的关系，确认收件需寄件人、收件人，支付、取消订单需寄件人
                if (action == OrderActionEnum.COMPLETE)
                    participant(order, customer, role, users.selectById(userId));
                else own(order, customer, role);
                if (action == OrderActionEnum.PAY) {
                    expect(previous, UNPAID);
                    order.setOrderStatus(AVAILABLE);
                    order.setPaymentStatus(PAID);
                    description = "支付成功";
                } else if (action == OrderActionEnum.COMPLETE) {
                    expect(previous, AWAITING_COLLECTION);
                    order.setOrderStatus(COMPLETED);
                    description = "收寄件人确认取件";
                } else {
                    if (previous != UNPAID && previous != AVAILABLE)
                        throw invalid("接单后请联系管理员取消");
                    cancel(order, reason);
                    description = reason;
                }
            }
            //配送员操作
            case ACCEPT, PICKUP, DELIVER -> {
                //校验角色是否为配送员
                RoleAccount courier = guard.requireRole(userId, role, UserRoleEnum.COURIER);
                if (action == OrderActionEnum.ACCEPT) {
                    expect(previous, AVAILABLE);
                    if (order.getCourierId() != null)
                        throw invalid("订单已被接单");
                    //检查配送员是不是订单发布者本人
                    boolean self = orders.selectCount(new LambdaQueryWrapper<ExpressOrder>()
                            .eq(ExpressOrder::getId, id)
                            .apply("customer_id in (select id from customer where user_id = {0})", userId)) > 0;
                    if (self) throw invalid("不能接自己发布的订单");

                    order.setCourierId(courier.getId());
                    order.setOrderStatus(AWAITING_PICKUP);
                    description = "配送员接单";
                } else {
                    own(order, courier, role);
                    if (pending(id)) throw invalid("异常待处理，暂不能继续配送");
                    boolean pickup = action == OrderActionEnum.PICKUP;
                    expect(previous, pickup ? AWAITING_PICKUP : DELIVERING);
                    order.setOrderStatus(pickup ? DELIVERING : AWAITING_COLLECTION);
                    description = pickup ? "配送员确认揽收" : "配送员确认送达";
                }
            }
            //管理员操作
            case ADMIN_CANCEL -> {
                //校验角色是否为管理员
                guard.requireRole(userId, role, UserRoleEnum.ADMIN);
                if (previous == COMPLETED || previous == CANCELLED)
                    throw invalid("已结束的订单不能取消");
                cancel(order, reason);
                description = reason;
            }
            default -> throw invalid("未知操作");
        }
        //更新订单失败
        if (orders.updateById(order) != 1)
            throw invalid("订单状态已变化，请刷新后重试");
        //管理员取消订单时关闭待处理异常
        if (action == OrderActionEnum.ADMIN_CANCEL) {
            Long adminId = guard.requireRole(userId, role, UserRoleEnum.ADMIN).getId();
            for (DeliveryException exception : history(id)) {
                if (exception.getStatus() == ExceptionStatusEnum.PENDING)
                    close(exception, adminId, ExceptionResolutionEnum.CANCEL, reason);
            }
        }
        record(order, previous, userId, role, description);
    }

    //配送员上报异常
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportException(Long userId, UserRoleEnum role, Long id, ReportExceptionDTO dto) {
        //校验配送员身份及其与订单的关系
        RoleAccount courier = guard.requireRole(userId, role, UserRoleEnum.COURIER);
        ExpressOrder order = get(id);
        own(order, courier, role);
        //校验订单状态和待处理异常
        if (order.getOrderStatus() != AWAITING_PICKUP && order.getOrderStatus() != DELIVERING)
            throw invalid("仅待揽收或配送中可以上报异常");
        if (pending(id)) throw invalid("该订单已有待处理异常");
        validateDescription(dto.getDescription());
        //先锁定订单版本，再保存异常和流转记录
        touch(order);
        DeliveryException exception = new DeliveryException();
        exception.setOrderId(id);
        exception.setCourierId(courier.getId());
        exception.setType(dto.getType());
        exception.setDescription(dto.getDescription().trim());
        exception.setStatus(ExceptionStatusEnum.PENDING);
        if (exceptions.insert(exception) != 1) throw invalid("保存异常失败");
        record(order, order.getOrderStatus(), userId, role, "配送员上报异常，配送暂停");
        return exception.getId();
    }

    //管理员查询异常列表
    @Override
    public PageResult<DeliveryException> listExceptions(Long userId, UserRoleEnum role, ExceptionQueryDTO dto) {
        guard.requireRole(userId, role, UserRoleEnum.ADMIN);
        //按处理状态或订单ID筛选并按创建时间倒序分页查询
        return PageResult.of(exceptions.selectPage(PageResult.pageOf(dto.getCurrentPage()),
                new LambdaQueryWrapper<DeliveryException>()
                        .eq(dto.getStatus() != null, DeliveryException::getStatus, dto.getStatus())
                        .eq(dto.getOrderId() != null, DeliveryException::getOrderId, dto.getOrderId())
                        .orderByDesc(DeliveryException::getCreateTime).orderByDesc(DeliveryException::getId)));
    }

    //订单参与者或管理员查看单条配送异常
    @Override
    public DeliveryException exceptionDetail(Long userId, UserRoleEnum role, Long id) {
        DeliveryException exception = getException(id);
        RoleAccount account = guard.requireActiveAccount(userId, role);
        ExpressOrder order = get(exception.getOrderId());
        //管理员可直接查看，其他角色必须是订单参与者
        if (role != UserRoleEnum.ADMIN)
            participant(order, account, role,
                    role == UserRoleEnum.CUSTOMER ? users.selectById(userId) : null);
        return exception;
    }

    //管理员处理异常
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveException(Long userId, UserRoleEnum role, Long id, ResolveExceptionDTO dto) {
        //校验管理员身份以及异常和订单状态
        RoleAccount admin = guard.requireRole(userId, role, UserRoleEnum.ADMIN);
        DeliveryException exception = getException(id);
        ExpressOrder order = get(exception.getOrderId());
        if (exception.getStatus() != ExceptionStatusEnum.PENDING)
            throw invalid("异常已处理，请刷新");
        validateDescription(dto.getDescription());
        OrderStatusEnum previous = order.getOrderStatus();
        if (previous != AWAITING_PICKUP && previous != DELIVERING)
            throw invalid("订单状态不允许处理异常");
        //取消处理会修改订单状态，恢复处理保持原配送状态
        boolean cancelled = dto.getResolution() == ExceptionResolutionEnum.CANCEL;
        if (cancelled) cancel(order, dto.getDescription());
        //先锁定并更新订单，再关闭异常和保存流转记录
        touch(order);
        close(exception, admin.getId(), dto.getResolution(), dto.getDescription());
        record(order, previous, userId, role, cancelled ? "异常处理：取消订单" : "异常处理：恢复配送");
    }

    //查询订单
    private ExpressOrder get(Long id) {
        ExpressOrder order = orders.selectById(id);
        if (order == null) throw invalid("订单不存在");
        return order;
    }

    //校验当前账号是否是订单的寄件人或配送员
    private void own(ExpressOrder order, RoleAccount account, UserRoleEnum role) {
        Long owner = role == UserRoleEnum.CUSTOMER ? order.getCustomerId() : order.getCourierId();
        if (!Objects.equals(owner, account.getId()))
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
    }

    //校验当前账号是否是订单的收件人
    private static boolean recipient(ExpressOrder order, User user) {
        return Objects.equals(order.getDeliveryPhone(), user.getPhone());
    }

    //校验订单访问权限
    private void participant(ExpressOrder order, RoleAccount account, UserRoleEnum role, User user) {
        if (role == UserRoleEnum.CUSTOMER && recipient(order, user)) return;
        own(order, account, role);
    }

    //批量回填订单中的配送员姓名和手机号
    private void attachCouriers(List<ExpressOrder> list) {
        Set<Long> courierIds = list.stream().map(ExpressOrder::getCourierId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (courierIds.isEmpty()) return;

        Map<Long, Long> courierToUser = couriers.selectList(
                        new LambdaQueryWrapper<Courier>().in(Courier::getId, courierIds))
                .stream().collect(Collectors.toMap(Courier::getId, Courier::getUserId));
        if (courierToUser.isEmpty()) return;

        Map<Long, User> userById = users.selectList(
                        new LambdaQueryWrapper<User>().in(User::getId, courierToUser.values()))
                .stream().collect(Collectors.toMap(User::getId, user -> user));
        list.forEach(order -> {
            User courier = userById.get(courierToUser.get(order.getCourierId()));
            if (courier != null) {
                order.setCourierName(courier.getUsername());
                order.setCourierPhone(courier.getPhone());
            }
        });
    }

    //清除接单大厅不应展示的联系人、电话和备注
    private void hideContacts(ExpressOrder order) {
        order.setPickupName(null);
        order.setPickupPhone(null);
        order.setDeliveryName(null);
        order.setDeliveryPhone(null);
        order.setRemark(null);
        order.setCustomerId(null);
    }

    //校验订单状态是否为预期状态
    private void expect(OrderStatusEnum actual, OrderStatusEnum expected) {
        if (actual != expected) throw invalid("订单状态已变化，请刷新后重试");
    }

    //设置订单取消状态并处理模拟退款
    private void cancel(ExpressOrder order, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 255)
            throw invalid("请填写255字以内的取消原因");
        order.setOrderStatus(CANCELLED);
        if (order.getPaymentStatus() == PAID) order.setPaymentStatus(REFUNDED);
    }

    //保存订单状态流转记录
    private void record(ExpressOrder order, OrderStatusEnum previous, Long userId, UserRoleEnum role, String description) {
        OrderStatusRecord record = new OrderStatusRecord();
        record.setOrderId(order.getId());
        record.setFromStatus(previous);
        record.setToStatus(order.getOrderStatus());
        record.setOperatorId(userId);
        record.setOperatorRole(role);
        record.setDescription(description);
        if (records.insert(record) != 1) throw invalid("保存订单记录失败");
    }

    //读取配送异常
    private DeliveryException getException(Long id) {
        DeliveryException exception = exceptions.selectById(id);
        if (exception == null) throw invalid("异常记录不存在");
        return exception;
    }

    //读取订单的配送异常历史
    private List<DeliveryException> history(Long orderId) {
        return exceptions.selectList(new LambdaQueryWrapper<DeliveryException>()
                .eq(DeliveryException::getOrderId, orderId)
                .orderByAsc(DeliveryException::getCreateTime).orderByAsc(DeliveryException::getId));
    }

    //判断订单是否存在待处理异常
    private boolean pending(Long orderId) {
        return exceptions.selectCount(new LambdaQueryWrapper<DeliveryException>()
                .eq(DeliveryException::getOrderId, orderId)
                .eq(DeliveryException::getStatus, ExceptionStatusEnum.PENDING)) > 0;
    }

    //校验配送异常说明
    private void validateDescription(String description) {
        if (description == null || description.isBlank() || description.length() > 255)
            throw invalid("请填写255字以内的说明");
    }

    //更新订单版本并校验并发修改
    private void touch(ExpressOrder order) {
        if (orders.updateById(order) != 1)
            throw invalid("订单状态已变化，请刷新后重试");
    }

    //关闭配送异常
    private void close(DeliveryException exception, Long adminId, ExceptionResolutionEnum resolution, String description) {
        exception.setStatus(ExceptionStatusEnum.RESOLVED);
        exception.setAdminId(adminId);
        exception.setResolution(resolution);
        exception.setResolutionDescription(description.trim());
        exception.setResolvedTime(new Date());
        if (exceptions.updateById(exception) != 1)
            throw invalid("保存处理结果失败");
    }
}
