package com.example.tracking_order.modules.cart.mapper;

import com.example.tracking_order.modules.cart.dto.CartDto;
import com.example.tracking_order.modules.cart.entity.Cart;
import com.example.tracking_order.modules.cart.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(target = "cartId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "summary", ignore = true)
    CartDto toCartDto(Cart cart);

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "productId", source = "item.product.id")
    @Mapping(target = "productName", source = "item.product.name")
    @Mapping(target = "productImage", source = "item.product.imageUrl")
    @Mapping(target = "productSku", source = "item.product.sku")
    @Mapping(target = "priceSnapshot", source = "item.priceSnapshot")
    @Mapping(target = "quantity", source = "item.quantity")
    @Mapping(target = "currentPrice", source = "currentPrice")
    @Mapping(target = "quantityInStock", source = "inStock")
    @Mapping(target = "isAvailable", source = "isAvailable")
    @Mapping(target = "subtotal", source = "itemSubtotal")
    CartDto.CartItemDto toCartItemDto(CartItem item, BigDecimal currentPrice, Integer inStock, Boolean isAvailable, BigDecimal itemSubtotal);
}
