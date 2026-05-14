package com.example.tracking_order.modules.cart.service;

import com.example.tracking_order.modules.cart.dto.CheckoutRequest;
import com.example.tracking_order.modules.cart.dto.CheckoutResponse;
import com.example.tracking_order.modules.cart.dto.QuickCheckoutRequest;

public interface CheckoutService {
    CheckoutResponse checkout(CheckoutRequest request);
    CheckoutResponse quickCheckout(QuickCheckoutRequest request);
}
