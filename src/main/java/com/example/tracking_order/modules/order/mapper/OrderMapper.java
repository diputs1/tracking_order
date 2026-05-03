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

    @Mapping(target = "orderCode", source = "orderCode")
    @Mapping(target = "paymentStatus", source = "paymentStatus")
    @Mapping(target = "paymentMethod", source = "paymentMethod")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "shippingFee", source = "shippingFee")
    @Mapping(target = "grandTotal", source = "grandTotal")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "shipping", source = "order")
    OrderDetailDto toOrderDetailDto(Order order);

    @Mapping(target = "order", source = "order")
    @Mapping(target = "paymentUrl", source = "paymentUrl")
    CheckoutResponse toCheckoutResponse(Order order, Payment payment, String paymentUrl);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "orderCode", source = "order.orderCode")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "paymentStatus", source = "payment.status")
    @Mapping(target = "discountAmount", source = "order.discountAmount")
    @Mapping(target = "shippingFee", source = "order.shippingFee")
    @Mapping(target = "grandTotal", source = "order.grandTotal")
    @Mapping(target = "estimatedDeliveryAt", expression = "java(java.time.LocalDateTime.now().plusDays(3))")
    CheckoutResponse.OrderSummary toOrderSummary(Order order, Payment payment);

    @Mapping(target = "recipientName", source = "receiverName")
    @Mapping(target = "recipientPhone", source = "receiverPhone")
    @Mapping(target = "address", expression = "java(order.getStreet() + \", \" + order.getWard() + \", \" + order.getDistrict() + \", \" + order.getProvince())")
    @Mapping(target = "carrierName", source = "carrier.name")
    @Mapping(target = "trackingNumber", source = "trackingNumber")
    @Mapping(target = "trackingUrl", source = "trackingUrl")
    OrderDetailDto.ShippingInfo toShippingInfo(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "subtotal", expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    OrderDetailDto.OrderItemDto toOrderItemDto(OrderItem item);
}
