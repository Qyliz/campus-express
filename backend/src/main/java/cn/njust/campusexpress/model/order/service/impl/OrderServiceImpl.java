package cn.njust.campusexpress.model.order.service.impl;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.order.entity.*;
import cn.njust.campusexpress.model.order.mapper.*;
import cn.njust.campusexpress.model.order.vo.OrderDetailVO;
import cn.njust.campusexpress.model.user.entity.*;
import cn.njust.campusexpress.model.user.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import cn.njust.campusexpress.model.user.mapper.UserMapper;
import static cn.njust.campusexpress.model.order.service.OrderState.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final ExpressOrderMapper orders;
    private final OrderStatusRecordMapper records;
    private final RoleAccountService accounts;
    private final UserMapper users;
    private final DeliveryExceptionMapper exceptions;

    // 使用数据库与账户相同的字符串比较规则；空联系方式永不参与匹配。
    private void recipientQuery(LambdaQueryWrapper<ExpressOrder> q, User user) {
        q.and(r -> {
            r.eq(ExpressOrder::getId, -1L);
            if (user != null && user.getPhone() != null && !user.getPhone().isBlank())
                r.or().eq(ExpressOrder::getDeliveryPhone, user.getPhone());
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank())
                r.or().eq(ExpressOrder::getDeliveryEmail, user.getEmail());
        });
    }

    private boolean recipient(ExpressOrder order, Long userId) {
        var q = new LambdaQueryWrapper<ExpressOrder>().eq(ExpressOrder::getId, order.getId());
        recipientQuery(q, users.selectById(userId));
        return orders.selectCount(q) > 0;
    }

    private void participant(ExpressOrder order, RoleAccount account, UserRoleEnum role, Long userId) {
        if (role == UserRoleEnum.CUSTOMER && recipient(order, userId)) return;
        own(order, account, role);
    }

    private List<DeliveryException> history(Long orderId) {
        return exceptions.selectList(new LambdaQueryWrapper<DeliveryException>()
            .eq(DeliveryException::getOrderId, orderId)
            .orderByAsc(DeliveryException::getCreateTime).orderByAsc(DeliveryException::getId));
    }

    private boolean pending(Long orderId) {
        return exceptions.selectCount(new LambdaQueryWrapper<DeliveryException>()
            .eq(DeliveryException::getOrderId, orderId).eq(DeliveryException::getStatus, 0)) > 0;
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ResultCodeEnum.PARAM_ERROR, message);
    }

    // 不使用管理员继承的角色列表判定业务身份。
    private RoleAccount requireRole(Long userId, UserRoleEnum role, UserRoleEnum expected) {
        if (role != expected) throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
        RoleAccount account = accounts.getByUserAndRole(userId, role);
        if (account == null || account.getStatus() != UserStatusEnum.NORMAL)
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION, "当前角色账户不可用");
        return account;
    }

    // 创建待支付订单，同时保存创建记录。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, UserRoleEnum role, CreateOrderDTO dto) {
        RoleAccount customer = requireRole(userId, role, UserRoleEnum.CUSTOMER);
        ExpressOrder order = new ExpressOrder();
        BeanUtils.copyProperties(dto, order);
        order.setCustomerId(customer.getId());
        order.setOrderStatus(UNPAID);
        order.setPaymentStatus(PAYMENT_UNPAID);
        order.setVersion(0);
        order.setCreateTime(new Date());
        order.setUpdateTime(order.getCreateTime());
        if (orders.insert(order) != 1) throw invalid("创建订单失败");
        record(order, null, userId, role, "创建订单");
        return order.getId();
    }

    // 按当前身份和列表范围分页查询，大厅结果隐藏联系信息。
    @Override
    public PageResult<ExpressOrder> list(Long userId, UserRoleEnum role, String scope, OrderQueryDTO dto) {
        LambdaQueryWrapper<ExpressOrder> query = new LambdaQueryWrapper<>();
        switch (scope) {
            case "mine" -> {
                Long customerId = requireRole(userId, role, UserRoleEnum.CUSTOMER).getId();
                User user = users.selectById(userId);
                switch (dto.getRelation()) {
                    case "created" -> query.eq(ExpressOrder::getCustomerId, customerId);
                    case "received" -> recipientQuery(query, user);
                    case "all" -> query.and(q -> {
                        q.eq(ExpressOrder::getCustomerId, customerId)
                            .or(r -> recipientQuery(r, user));
                    });
                    default -> throw invalid("未知订单关系");
                }
            }
            case "assigned" -> query.eq(ExpressOrder::getCourierId,
                    requireRole(userId, role, UserRoleEnum.COURIER).getId());
            case "available" -> {
                requireRole(userId, role, UserRoleEnum.COURIER);
                query.eq(ExpressOrder::getOrderStatus, AVAILABLE);
                // 也排除已经注销的本人收寄件人账户发布的订单。
                query.notInSql(ExpressOrder::getCustomerId,
                        "select id from customer where user_id = " + userId);
            }
            case "admin" -> requireRole(userId, role, UserRoleEnum.ADMIN);
            default -> throw invalid("未知列表");
        }
        query.eq(dto.getOrderStatus() != null, ExpressOrder::getOrderStatus, dto.getOrderStatus());
        if ("admin".equals(scope)) query.eq(dto.getOrderId() != null, ExpressOrder::getId, dto.getOrderId());
        query.orderByDesc(ExpressOrder::getCreateTime).orderByDesc(ExpressOrder::getId);
        Page<ExpressOrder> page = orders.selectPage(new Page<>(dto.getCurrentPage() == null ? 1 : dto.getCurrentPage(), 10), query);
        if (!page.getRecords().isEmpty() && !"available".equals(scope)) {
            List<Long> ids = page.getRecords().stream().map(ExpressOrder::getId).toList();
            Set<Long> pendingIds = exceptions.selectList(new LambdaQueryWrapper<DeliveryException>()
                .in(DeliveryException::getOrderId, ids).eq(DeliveryException::getStatus, 0))
                .stream().map(DeliveryException::getOrderId).collect(Collectors.toSet());
            page.getRecords().forEach(o -> o.setPendingException(pendingIds.contains(o.getId())));
            if (role == UserRoleEnum.CUSTOMER) {
                Long customerId = requireRole(userId, role, UserRoleEnum.CUSTOMER).getId();
                var receivedQuery = new LambdaQueryWrapper<ExpressOrder>().in(ExpressOrder::getId, ids);
                recipientQuery(receivedQuery, users.selectById(userId));
                Set<Long> receivedIds = orders.selectList(receivedQuery).stream().map(ExpressOrder::getId).collect(Collectors.toSet());
                page.getRecords().forEach(o -> {
                    o.setCreatedByMe(Objects.equals(customerId, o.getCustomerId()));
                    o.setReceivedByMe(receivedIds.contains(o.getId()));
                });
            }
        }
        if ("available".equals(scope)) page.getRecords().forEach(this::hideContacts);
        return PageResult.of(page);
    }

    // 查询订单，不存在时返回业务提示。
    private ExpressOrder get(Long id) {
        ExpressOrder order = orders.selectById(id);
        if (order == null) throw invalid("订单不存在");
        return order;
    }

    // 校验当前角色账户是否是订单的下单人或实际配送员。
    private void own(ExpressOrder order, RoleAccount account, UserRoleEnum role) {
        Long owner = role == UserRoleEnum.CUSTOMER ? order.getCustomerId() : order.getCourierId();
        if (!Objects.equals(owner, account.getId()))
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION);
    }

    // 校验查看权限，并返回订单详情和按时间排列的流转记录。
    @Override
    public OrderDetailVO detail(Long userId, UserRoleEnum role, Long id) {
        RoleAccount account = requireRole(userId, role, role);
        ExpressOrder order = get(id);
        if (role != UserRoleEnum.ADMIN) {
            // 大厅只提供摘要；完整详情仅向订单参与者开放。
            participant(order, account, role, userId);
        }
        List<DeliveryException> history = history(id);
        order.setPendingException(history.stream().anyMatch(e -> e.getStatus() == 0));
        List<String> actions = new ArrayList<>();
        int state = order.getOrderStatus();
        if (role == UserRoleEnum.ADMIN && state < COMPLETED) actions.add("admin-cancel");
        if (role == UserRoleEnum.CUSTOMER) {
            order.setCreatedByMe(Objects.equals(account.getId(), order.getCustomerId()));
            order.setReceivedByMe(recipient(order, userId));
            if (order.isCreatedByMe() && state == UNPAID) actions.add("pay");
            if (order.isCreatedByMe() && (state == UNPAID || state == AVAILABLE)) actions.add("cancel");
            if (state == AWAITING_COLLECTION) actions.add("complete");
        }
        boolean canReport = role == UserRoleEnum.COURIER && !order.isPendingException()
            && (state == AWAITING_PICKUP || state == DELIVERING);
        if (canReport) actions.add(state == AWAITING_PICKUP ? "pickup" : "deliver");
        return new OrderDetailVO(order, records.selectList(new LambdaQueryWrapper<OrderStatusRecord>()
                .eq(OrderStatusRecord::getOrderId, id)
                .orderByAsc(OrderStatusRecord::getCreateTime).orderByAsc(OrderStatusRecord::getId)), history, actions, canReport);
    }

    // 执行业务操作，通过乐观锁更新订单，并在同一事务中写入记录。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void act(Long userId, UserRoleEnum role, Long id, String action, String reason) {
        ExpressOrder order = get(id);
        int previous = order.getOrderStatus();
        String description;
        switch (action) {
            case "pay", "complete", "cancel" -> {
                RoleAccount customer = requireRole(userId, role, UserRoleEnum.CUSTOMER);
                if ("complete".equals(action)) participant(order, customer, role, userId);
                else own(order, customer, role);
                if ("pay".equals(action)) {
                    expect(previous, UNPAID);
                    order.setOrderStatus(AVAILABLE);
                    order.setPaymentStatus(PAID);
                    description = "模拟支付成功";
                } else if ("complete".equals(action)) {
                    expect(previous, AWAITING_COLLECTION);
                    order.setOrderStatus(COMPLETED);
                    description = "收寄件人确认取件";
                } else {
                    if (previous != UNPAID && previous != AVAILABLE) throw invalid("接单后请联系管理员取消");
                    cancel(order, reason);
                    description = reason;
                }
            }
            case "accept", "pickup", "deliver" -> {
                RoleAccount courier = requireRole(userId, role, UserRoleEnum.COURIER);
                if ("accept".equals(action)) {
                    expect(previous, AVAILABLE);
                    if (order.getCourierId() != null) throw invalid("订单已被接单");
                    // 原始用户关联查询包含逻辑删除账户，避免自己接自己的历史订单。
                    boolean self = orders.selectCount(new LambdaQueryWrapper<ExpressOrder>()
                            .eq(ExpressOrder::getId, id)
                            .inSql(ExpressOrder::getCustomerId, "select id from customer where user_id = " + userId)) > 0;
                    if (self) throw invalid("不能接自己发布的订单");
                    order.setCourierId(courier.getId());
                    order.setOrderStatus(AWAITING_PICKUP);
                    description = "配送员接单";
                } else {
                    own(order, courier, role);
                    if (pending(id)) throw invalid("异常待处理，暂不能继续配送");
                    expect(previous, "pickup".equals(action) ? AWAITING_PICKUP : DELIVERING);
                    order.setOrderStatus("pickup".equals(action) ? DELIVERING : AWAITING_COLLECTION);
                    description = "pickup".equals(action) ? "配送员确认揽收" : "配送员确认送达";
                }
            }
            case "admin-cancel" -> {
                requireRole(userId, role, UserRoleEnum.ADMIN);
                if (previous == COMPLETED || previous == CANCELLED) throw invalid("已结束的订单不能取消");
                cancel(order, reason);
                description = reason;
            }
            default -> throw invalid("未知操作");
        }
        order.setUpdateTime(new Date());
        // @Version 插件将旧 version 加入 WHERE，并在成功时递增版本。
        if (orders.updateById(order) != 1)
            throw invalid("订单状态已变化，请刷新后重试");
        if ("admin-cancel".equals(action)) {
            for (DeliveryException exception : history(id)) {
                if (exception.getStatus() == 0)
                    close(exception, requireRole(userId, role, UserRoleEnum.ADMIN).getId(), "CANCEL", reason);
            }
        }
        record(order, previous, userId, role, description);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportException(Long userId, UserRoleEnum role, Long id, ReportExceptionDTO dto) {
        RoleAccount courier = requireRole(userId, role, UserRoleEnum.COURIER);
        ExpressOrder order = get(id);
        own(order, courier, role);
        if (order.getOrderStatus() != AWAITING_PICKUP && order.getOrderStatus() != DELIVERING)
            throw invalid("仅待揽收或配送中可以上报异常");
        if (pending(id)) throw invalid("该订单已有待处理异常");
        if (!Set.of("CONTACT", "ADDRESS", "ITEM", "COURIER", "OTHER").contains(dto.getType())) throw invalid("异常类型无效");
        validateDescription(dto.getDescription());
        touch(order);
        DeliveryException exception = new DeliveryException();
        exception.setOrderId(id);
        exception.setCourierId(courier.getId());
        exception.setType(dto.getType());
        exception.setDescription(dto.getDescription().trim());
        exception.setStatus(0);
        exception.setCreateTime(new Date());
        if (exceptions.insert(exception) != 1) throw invalid("保存异常失败");
        record(order, order.getOrderStatus(), userId, role, "配送员上报异常，配送暂停");
        return exception.getId();
    }

    @Override
    public PageResult<DeliveryException> listExceptions(Long userId, UserRoleEnum role, ExceptionQueryDTO dto) {
        requireRole(userId, role, UserRoleEnum.ADMIN);
        return PageResult.of(exceptions.selectPage(new Page<>(dto.getCurrentPage(), 10),
            new LambdaQueryWrapper<DeliveryException>()
                .eq(dto.getStatus() != null, DeliveryException::getStatus, dto.getStatus())
                .eq(dto.getOrderId() != null, DeliveryException::getOrderId, dto.getOrderId())
                .orderByDesc(DeliveryException::getCreateTime).orderByDesc(DeliveryException::getId)));
    }

    @Override
    public DeliveryException exceptionDetail(Long userId, UserRoleEnum role, Long id) {
        DeliveryException exception = getException(id);
        RoleAccount account = requireRole(userId, role, role);
        ExpressOrder order = get(exception.getOrderId());
        if (role != UserRoleEnum.ADMIN) participant(order, account, role, userId);
        return exception;
    }

    private DeliveryException getException(Long id) {
        DeliveryException exception = exceptions.selectById(id);
        if (exception == null) throw invalid("异常记录不存在");
        return exception;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveException(Long userId, UserRoleEnum role, Long id, ResolveExceptionDTO dto) {
        RoleAccount admin = requireRole(userId, role, UserRoleEnum.ADMIN);
        // 先读取订单版本，再读取异常，确保与所有订单操作竞争同一个版本。
        ExpressOrder order = get(getException(id).getOrderId());
        DeliveryException exception = getException(id);
        if (exception.getStatus() != 0) throw invalid("异常已处理，请刷新");
        validateDescription(dto.getDescription());
        int previous = order.getOrderStatus();
        if (previous != AWAITING_PICKUP && previous != DELIVERING) throw invalid("订单状态不允许处理异常");
        if ("CANCEL".equals(dto.getResolution())) cancel(order, dto.getDescription());
        else if (!"RESUME".equals(dto.getResolution())) throw invalid("处理结果无效");
        touch(order);
        close(exception, admin.getId(), dto.getResolution(), dto.getDescription());
        record(order, previous, userId, role, "CANCEL".equals(dto.getResolution()) ? "异常处理：取消订单" : "异常处理：恢复配送");
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank() || description.length() > 255)
            throw invalid("请填写255字以内的说明");
    }

    private void touch(ExpressOrder order) {
        order.setUpdateTime(new Date());
        if (orders.updateById(order) != 1) throw invalid("订单状态已变化，请刷新后重试");
    }

    private void close(DeliveryException exception, Long adminId, String resolution, String description) {
        exception.setStatus(1);
        exception.setAdminId(adminId);
        exception.setResolution(resolution);
        exception.setResolutionDescription(description.trim());
        exception.setResolvedTime(new Date());
        if (exceptions.updateById(exception) != 1) throw invalid("保存处理结果失败");
    }

    // 阻止重复操作和跳过配送节点。
    private void expect(int actual, int expected) {
        if (actual != expected) throw invalid("订单状态已变化，请刷新后重试");
    }

    // 设置取消状态，已支付订单同步标记模拟退款，实际保存由调用方完成。
    private void cancel(ExpressOrder order, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 255) throw invalid("请填写255字以内的取消原因");
        order.setOrderStatus(CANCELLED);
        if (order.getPaymentStatus() == PAID) order.setPaymentStatus(REFUNDED);
    }

    // 保存一次成功的状态流转及操作人信息。
    private void record(ExpressOrder order, Integer previous, Long userId, UserRoleEnum role, String description) {
        OrderStatusRecord record = new OrderStatusRecord();
        record.setOrderId(order.getId());
        record.setFromStatus(previous);
        record.setToStatus(order.getOrderStatus());
        record.setOperatorId(userId);
        record.setOperatorRole(role);
        record.setDescription(description);
        record.setCreateTime(new Date());
        if (records.insert(record) != 1) throw invalid("保存订单记录失败");
    }

    // 清除接单大厅不应展示的联系人、电话和备注。
    private void hideContacts(ExpressOrder order) {
        order.setPickupName(null);
        order.setPickupPhone(null);
        order.setDeliveryName(null);
        order.setDeliveryPhone(null);
        order.setDeliveryEmail(null);
        order.setRemark(null);
        order.setCustomerId(null);
    }
}
