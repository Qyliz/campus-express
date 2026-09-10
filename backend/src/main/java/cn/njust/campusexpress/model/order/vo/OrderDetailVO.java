package cn.njust.campusexpress.model.order.vo;
import cn.njust.campusexpress.model.order.entity.*;
import java.util.List;
public record OrderDetailVO(ExpressOrder order, List<OrderStatusRecord> records,
    List<DeliveryException> exceptions, List<String> allowedActions, boolean canReportException) {}
