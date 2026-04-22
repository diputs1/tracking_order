package com.example.tracking_order.modules.cart.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.modules.cart.dto.*;
import com.example.tracking_order.modules.cart.entity.Cart;
import com.example.tracking_order.modules.cart.entity.CartItem;
import com.example.tracking_order.modules.cart.repository.CartItemRepository;
import com.example.tracking_order.modules.cart.repository.CartRepository;
import com.example.tracking_order.modules.cart.service.CartService;
import com.example.tracking_order.modules.catalog.entity.Inventory;
import com.example.tracking_order.modules.catalog.entity.Product;
import com.example.tracking_order.modules.catalog.enums.ProductStatus;
import com.example.tracking_order.modules.catalog.repository.InventoryRepository;
import com.example.tracking_order.modules.catalog.repository.ProductRepository;
import com.example.tracking_order.modules.discount.entity.Discount;
import com.example.tracking_order.modules.discount.enums.DiscountType;
import com.example.tracking_order.modules.discount.repository.DiscountRepository;
import com.example.tracking_order.modules.order.entity.Order;
import com.example.tracking_order.modules.order.entity.OrderItem;
import com.example.tracking_order.modules.order.enums.OrderStatus;
import com.example.tracking_order.modules.order.repository.OrderItemRepository;
import com.example.tracking_order.modules.order.repository.OrderRepository;
import com.example.tracking_order.modules.payment.entity.Payment;
import com.example.tracking_order.modules.payment.enums.PaymentMethod;
import com.example.tracking_order.modules.payment.enums.PaymentStatus;
import com.example.tracking_order.modules.payment.repository.PaymentRepository;
import com.example.tracking_order.modules.shipping.entity.ShippingCarrier;
import com.example.tracking_order.modules.shipping.repository.ShippingCarrierRepository;
import com.example.tracking_order.modules.user.entity.Address;
import com.example.tracking_order.modules.user.entity.User;
import com.example.tracking_order.modules.user.repository.AddressRepository;
import com.example.tracking_order.modules.user.repository.UserRepository;
import com.example.tracking_order.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ShippingCarrierRepository shippingCarrierRepository;
    private final DiscountRepository discountRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Người dùng không tồn tại"));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartDto.CartItemDto> itemDtos = new ArrayList<>();
        
        int totalQty = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasOutOfStock = false;
        
        for (CartItem item : items) {
            Product product = item.getProduct();
            Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
            
            int inStock = inventory != null ? inventory.getQuantityInStock() - inventory.getQuantityReserved() : 0;
            boolean isAvailable = inStock >= item.getQuantity() && product.getStatus() == ProductStatus.ACTIVE;
            
            if (!isAvailable) {
                hasOutOfStock = true;
            }
            
            BigDecimal currentPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();
            BigDecimal itemSubtotal = currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            
            totalQty += item.getQuantity();
            if (isAvailable) {
                subtotal = subtotal.add(itemSubtotal);
            }
            
            itemDtos.add(CartDto.CartItemDto.builder()
                    .item_id(item.getId())
                    .product_id(product.getId())
                    .product_name(product.getName())
                    .product_sku(product.getSku())
                    .price_snapshot(item.getPriceSnapshot())
                    .current_price(currentPrice)
                    .quantity(item.getQuantity())
                    .quantity_in_stock(inStock)
                    .is_available(isAvailable)
                    .subtotal(itemSubtotal)
                    .build());
        }

        CartDto.Summary summary = CartDto.Summary.builder()
                .item_count(items.size())
                .total_qty(totalQty)
                .subtotal(subtotal)
                .has_out_of_stock(hasOutOfStock)
                .build();

        return CartDto.builder()
                .cart_id(cart.getId())
                .items(itemDtos)
                .summary(summary)
                .build();
    }

    @Override
    @Transactional
    public CartDto addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        Product product = productRepository.findById(request.getProduct_id())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không tồn tại"));
                
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Sản phẩm ngừng kinh doanh");
        }
        
        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không có thông tin tồn kho"));
                
        int available = inventory.getQuantityInStock() - inventory.getQuantityReserved();
        
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
        
        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (newQuantity > available) {
                throw new AppException(ErrorCode.OUT_OF_STOCK, "Tồn kho không đủ");
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setPriceSnapshot(product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice());
            cartItemRepository.save(existingItem);
        } else {
            if (request.getQuantity() > available) {
                throw new AppException(ErrorCode.OUT_OF_STOCK, "Tồn kho không đủ");
            }
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .priceSnapshot(product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice())
                    .build();
            cartItemRepository.save(newItem);
        }
        
        return getCart();
    }

    @Override
    @Transactional
    public CartDto updateCartItem(Long itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không có trong giỏ"));
                
        if (request.getQuantity() == 0) {
            cartItemRepository.delete(item);
            return getCart();
        }
        
        Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không có thông tin tồn kho"));
                
        int available = inventory.getQuantityInStock() - inventory.getQuantityReserved();
        if (request.getQuantity() > available) {
            throw new AppException(ErrorCode.OUT_OF_STOCK, "Tồn kho không đủ");
        }
        
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        
        return getCart();
    }

    @Override
    @Transactional
    public CartDto removeCartItem(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không có trong giỏ"));
        cartItemRepository.delete(item);
        return getCart();
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Giỏ hàng trống");
        }
        
        return processCheckout(user, cartItems, request.getAddress_id(), request.getCarrier_id(), 
                request.getPayment_method(), request.getDiscount_code(), request.getNote(), cart.getId());
    }
    
    @Override
    @Transactional
    public CheckoutResponse quickCheckout(QuickCheckoutRequest request) {
        User user = getCurrentUser();
        
        Product product = productRepository.findById(request.getProduct_id())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không tồn tại"));
                
        CartItem mockItem = CartItem.builder()
                .product(product)
                .quantity(request.getQuantity())
                .priceSnapshot(product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice())
                .build();
                
        List<CartItem> items = List.of(mockItem);
        
        return processCheckout(user, items, request.getAddress_id(), request.getCarrier_id(), 
                request.getPayment_method(), request.getDiscount_code(), null, null);
    }

    private CheckoutResponse processCheckout(User user, List<CartItem> items, Long addressId, Long carrierId, 
                                             PaymentMethod paymentMethod, String discountCode, String note, Long cartId) {
        
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Địa chỉ không tồn tại"));
                
        ShippingCarrier carrier = shippingCarrierRepository.findById(carrierId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn vị vận chuyển không tồn tại"));

        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Sản phẩm " + product.getName() + " ngừng kinh doanh");
            }
            
            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không có thông tin tồn kho cho " + product.getName()));
                    
            int available = inventory.getQuantityInStock() - inventory.getQuantityReserved();
            if (item.getQuantity() > available) {
                throw new AppException(ErrorCode.OUT_OF_STOCK, "Sản phẩm " + product.getName() + " không đủ số lượng");
            }
            
            BigDecimal currentPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();
            subtotal = subtotal.add(currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            
            inventory.setQuantityReserved(inventory.getQuantityReserved() + item.getQuantity());
            inventoryRepository.save(inventory);
        }
        
        BigDecimal discountAmount = BigDecimal.ZERO;
        Discount appliedDiscount = null;
        if (discountCode != null && !discountCode.isEmpty()) {
            appliedDiscount = discountRepository.findByCodeAndIsActiveTrue(discountCode)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Mã giảm giá không hợp lệ"));
                    
            LocalDateTime now = LocalDateTime.now();
            if (appliedDiscount.getStartAt().isAfter(now) || appliedDiscount.getExpiresAt().isBefore(now)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Mã giảm giá đã hết hạn hoặc chưa áp dụng");
            }
            if (subtotal.compareTo(appliedDiscount.getMinOrderValue()) < 0) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã");
            }
            
            if (appliedDiscount.getType() == DiscountType.PERCENTAGE) {
                discountAmount = subtotal.multiply(appliedDiscount.getValue()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            } else {
                discountAmount = appliedDiscount.getValue();
            }
            
            if (appliedDiscount.getMaxDiscountAmount() != null && discountAmount.compareTo(appliedDiscount.getMaxDiscountAmount()) > 0) {
                discountAmount = appliedDiscount.getMaxDiscountAmount();
            }
            
            appliedDiscount.setUsedCount(appliedDiscount.getUsedCount() + 1);
            discountRepository.save(appliedDiscount);
        }
        
        BigDecimal shippingFee = carrier.getBaseFee();
        
        BigDecimal grandTotal = subtotal.subtract(discountAmount).add(shippingFee);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }
        
        String orderCode = "ORD-" + System.currentTimeMillis();
        
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .grandTotal(grandTotal)
                .address(address)
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .street(address.getStreet())
                .carrier(carrier)
                .discount(appliedDiscount)
                .note(note)
                .paymentMethod(paymentMethod)
                .build();
                
        order = orderRepository.save(order);
        
        for (CartItem item : items) {
            Product product = item.getProduct();
            BigDecimal currentPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();
            
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .unitPrice(currentPrice)
                    .quantity(item.getQuantity())
                    .subtotal(currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                    .seller(product.getSeller())
                    .sellerName(product.getSeller().getFullName())
                    .build();
            orderItemRepository.save(orderItem);
        }
        
        Payment payment = Payment.builder()
                .order(order)
                .method(paymentMethod)
                .status(PaymentStatus.UNPAID)
                .amount(grandTotal)
                .build();
        paymentRepository.save(payment);
        
        if (cartId != null) {
            cartItemRepository.deleteByCartId(cartId);
        }
        
        String paymentUrl = null;
        if (paymentMethod != PaymentMethod.COD) {
            paymentUrl = "https://payment.sandbox.com/pay/" + orderCode;
        }
        
        return CheckoutResponse.builder()
                .order(CheckoutResponse.OrderSummary.builder()
                        .id(order.getId())
                        .order_code(order.getOrderCode())
                        .status(order.getStatus().name())
                        .payment_status(payment.getStatus().name())
                        .subtotal(order.getSubtotal())
                        .discount_amount(order.getDiscountAmount())
                        .shipping_fee(order.getShippingFee())
                        .grand_total(order.getGrandTotal())
                        .estimated_delivery_at(LocalDateTime.now().plusDays(3))
                        .build())
                .payment_url(paymentUrl)
                .build();
    }
}
