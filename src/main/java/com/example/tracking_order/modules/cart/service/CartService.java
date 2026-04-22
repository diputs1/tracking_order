package com.example.tracking_order.modules.cart.service;

import com.example.tracking_order.modules.cart.dto.*;

public interface CartService {
    CartDto getCart();
    CartDto addToCart(AddToCartRequest request);
    CartDto updateCartItem(Long itemId, UpdateCartItemRequest request);
    CartDto removeCartItem(Long itemId);
    CheckoutResponse checkout(CheckoutRequest request);
    CheckoutResponse quickCheckout(QuickCheckoutRequest request);
}
