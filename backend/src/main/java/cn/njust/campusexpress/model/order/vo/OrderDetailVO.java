package cn.njust.campusexpress.model.order.vo;

import cn.njust.campusexpress.model.order.entity.DeliveryException;
import cn.njust.campusexpress.model.order.entity.ExpressOrder;
import cn.njust.campusexpress.model.order.entity.OrderStatusRecord;
import lombok.Data;

import java.util.List;

//订单详情VO
@Data
public class OrderDetailVO {
    private ExpressOrder order;
    private List<OrderStatusRecord> records;
    private List<DeliveryException> exceptions;
    private List<String> allowedActions;
    private boolean canReportException;
}
