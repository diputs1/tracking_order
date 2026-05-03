package com.example.tracking_order.modules.order.mapper;

import com.example.tracking_order.modules.cart.dto.CheckoutResponse;
import com.example.tracking_order.modules.order.dto.OrderDetailDto;
import com.example.tracking_order.modules.order.dto.OrderListDto;
import com.example.tracking_order.modules.order.entity.Order;
import com.example.tracking_order.modules.order.entity.OrderItem;
import com.example.tracking_order.modules.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "order_code", source = "orderCode")
    @Mapping(target = "payment_status", source = "paymentStatus")
    @Mapping(target = "payment_method", source = "paymentMethod")
    @Mapping(target = "discount_amount", source = "discountAmount")
    @Mapping(target = "shipping_fee", source = "shippingFee")
    @Mapping(target = "grand_total", source = "grandTotal")
    @Mapping(target = "created_at", source = "createdAt")
    @Mapping(target = "updated_at", source = "updatedAt")
    @Mapping(target = "shipping", source = "order")
    OrderDetailDto toOrderDetailDto(Order order);

    @Mapping(target = "order", source = "order")
    @Mapping(target = "payment_url", source = "paymentUrl")
    CheckoutResponse toCheckoutResponse(Order order, Payment payment, String paymentUrl);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "order_code", source = "order.orderCode")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "payment_status", source = "payment.status")
    @Mapping(target = "discount_amount", source = "order.discountAmount")
    @Mapping(target = "shipping_fee", source = "order.shippingFee")
    @Mapping(target = "grand_total", source = "order.grandTotal")
    @Mapping(target = "estimated_delivery_at", expression = "java(java.time.LocalDateTime.now().plusDays(3))")
    CheckoutResponse.OrderSummary toOrderSummary(Order order, Payment payment);

    @Mapping(target = "recipient_name", source = "receiverName")
    @Mapping(target = "recipient_phone", source = "receiverPhone")
    @Mapping(target = "address", expression = "java(order.getStreet() + \", \" + order.getWard() + \", \" + order.getDistrict() + \", \" + order.getProvince())")
    @Mapping(target = "carrier_name", source = "carrier.name")
    @Mapping(target = "tracking_number", source = "trackingNumber")
    @Mapping(target = "tracking_url", source = "trackingUrl")
    OrderDetailDto.ShippingInfo toShippingInfo(Order order);

    @Mapping(target = "product_id", source = "product.id")
    @Mapping(target = "product_name", source = "product.name")
    @Mapping(target = "product_sku", source = "product.sku")
    @Mapping(target = "unit_price", source = "unitPrice")
    @Mapping(target = "subtotal", expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    OrderDetailDto.OrderItemDto toOrderItemDto(OrderItem item);
}
