package cn.njust.campusexpress.model.order.service;
/** 数据库及接口中的订单状态值。 */
public final class OrderState {
    private OrderState() {}
    public static final int UNPAID=0, AVAILABLE=1, AWAITING_PICKUP=2,
        DELIVERING=3, AWAITING_COLLECTION=4, COMPLETED=5, CANCELLED=6;
    public static final int PAYMENT_UNPAID=0, PAID=1, REFUNDED=2;
}

