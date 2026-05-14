package com.example.tracking_order.modules.cart.controller;

import com.example.tracking_order.common.response.ApiResponse;
import com.example.tracking_order.modules.cart.dto.*;
import com.example.tracking_order.modules.cart.service.CartService;
import com.example.tracking_order.modules.cart.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    @GetMapping("/cart")
    public ApiResponse<CartDto> getCart() {
        return ApiResponse.success(cartService.getCart());
    }

    @PostMapping("/cart/items")
    public ApiResponse<CartDto> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return ApiResponse.success(cartService.addToCart(request));
    }

    @PatchMapping("/cart/items/{itemId}")
    public ApiResponse<CartDto> updateCartItem(@PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateCartItem(itemId, request));
    }

    @DeleteMapping("/cart/items/{itemId}")
    public ApiResponse<CartDto> removeCartItem(@PathVariable Long itemId) {
        return ApiResponse.success(cartService.removeCartItem(itemId));
    }

    @PostMapping("/cart/checkout")
    public ApiResponse<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.success(checkoutService.checkout(request));
    }

    @PostMapping("/orders/quick-checkout")
    public ApiResponse<CheckoutResponse> quickCheckout(@Valid @RequestBody QuickCheckoutRequest request) {
        return ApiResponse.success(checkoutService.quickCheckout(request));
    }
}
