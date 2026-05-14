package com.example.tracking_order.modules.cart.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.modules.cart.dto.CheckoutRequest;
import com.example.tracking_order.modules.cart.dto.CheckoutResponse;
import com.example.tracking_order.modules.cart.dto.QuickCheckoutRequest;
import com.example.tracking_order.modules.cart.entity.Cart;
import com.example.tracking_order.modules.cart.entity.CartItem;
import com.example.tracking_order.modules.cart.repository.CartItemRepository;
import com.example.tracking_order.modules.cart.repository.CartRepository;
import com.example.tracking_order.modules.cart.service.CheckoutService;
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
import com.example.tracking_order.modules.order.mapper.OrderMapper;
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
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

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
    private final OrderMapper orderMapper;

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Người dùng không tồn tại"));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckoutResponse checkout(CheckoutRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Giỏ hàng trống");
        }

        return processCheckout(user, cartItems, request.getAddressId(), request.getCarrierId(),
                request.getPaymentMethod(), request.getDiscountCode(), request.getNote(), cart.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckoutResponse quickCheckout(QuickCheckoutRequest request) {
        User user = getCurrentUser();

        if (request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Số lượng sản phẩm phải lớn hơn 0");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không tồn tại"));

        CartItem mockItem = CartItem.builder()
                .product(product)
                .quantity(request.getQuantity())
                .priceSnapshot(product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice())
                .build();

        List<CartItem> items = List.of(mockItem);

        return processCheckout(user, items, request.getAddressId(), request.getCarrierId(),
                request.getPaymentMethod(), request.getDiscountCode(), null, null);
    }

    private CheckoutResponse processCheckout(User user, List<CartItem> items, Long addressId, Long carrierId,
            PaymentMethod paymentMethod, String discountCode, String note, Long cartId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Địa chỉ không tồn tại"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Địa chỉ không thuộc về người dùng");
        }

        ShippingCarrier carrier = shippingCarrierRepository.findById(carrierId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn vị vận chuyển không tồn tại"));

        // 1. Validate and reserve stock
        BigDecimal subtotal = validateAndReserveInventory(items);

        // 2. Apply discount
        DiscountResult discountResult = calculateDiscount(discountCode, subtotal);
        BigDecimal discountAmount = discountResult.amount();
        Discount appliedDiscount = discountResult.discount();

        // 3. Calculate totals
        BigDecimal shippingFee = carrier.getBaseFee();
        BigDecimal grandTotal = subtotal.subtract(discountAmount).add(shippingFee);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        // 4. Create order
        Order order = saveOrder(user, address, carrier, appliedDiscount, subtotal,
                discountAmount, shippingFee, grandTotal, paymentMethod, note);

        // 5. Create order items
        saveOrderItems(order, items);

        // 6. Create payment
        Payment payment = createPayment(order, paymentMethod, grandTotal);

        // 7. Clear cart
        if (cartId != null) {
            cartItemRepository.deleteByCartId(cartId);
        }

        String paymentUrl = generatePaymentUrl(order.getOrderCode(), paymentMethod);
        return orderMapper.toCheckoutResponse(order, payment, paymentUrl);
    }

    private BigDecimal validateAndReserveInventory(List<CartItem> items) {
        List<Long> productIds = items.stream()
                .map(item -> item.getProduct().getId())
                .toList();

        List<Inventory> inventories = inventoryRepository.findByProductIdInWithLock(productIds);
        HashMap<Long, Inventory> inventoryMap = new HashMap<>();
        for (Inventory inv : inventories) {
            inventoryMap.put(inv.getProduct().getId(), inv);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "Sản phẩm " + product.getName() + " ngừng kinh doanh");
            }

            Inventory inventory = inventoryMap.get(product.getId());
            if (inventory == null) {
                throw new AppException(ErrorCode.NOT_FOUND, "Không có thông tin tồn kho cho " + product.getName());
            }

            int available = inventory.getQuantityInStock() - inventory.getQuantityReserved();
            if (item.getQuantity() > available) {
                throw new AppException(ErrorCode.OUT_OF_STOCK, "Sản phẩm " + product.getName() + " không đủ số lượng");
            }

            BigDecimal currentPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();
            subtotal = subtotal.add(currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())));

            inventory.setQuantityReserved(inventory.getQuantityReserved() + item.getQuantity());
        }
        inventoryRepository.saveAll(inventories);
        return subtotal;
    }

    private DiscountResult calculateDiscount(String code, BigDecimal subtotal) {
        if (code == null || code.isEmpty()) {
            return new DiscountResult(BigDecimal.ZERO, null);
        }

        Discount appliedDiscount = discountRepository.findByCodeAndIsActiveTrueWithLock(code)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Mã giảm giá không hợp lệ"));

        LocalDateTime now = LocalDateTime.now();
        if (appliedDiscount.getStartAt().isAfter(now) || appliedDiscount.getExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Mã giảm giá đã hết hạn hoặc chưa áp dụng");
        }
        if (subtotal.compareTo(appliedDiscount.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã");
        }

        if (appliedDiscount.getMaxUses() != null && appliedDiscount.getUsedCount() >= appliedDiscount.getMaxUses()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Mã giảm giá đã hết lượt sử dụng");
        }

        BigDecimal discountAmount;
        if (appliedDiscount.getType() == DiscountType.PERCENTAGE) {
            discountAmount = subtotal.multiply(appliedDiscount.getValue()).divide(BigDecimal.valueOf(100),
                    RoundingMode.HALF_UP);
        } else {
            discountAmount = appliedDiscount.getValue();
        }

        if (appliedDiscount.getMaxDiscountAmount() != null
                && discountAmount.compareTo(appliedDiscount.getMaxDiscountAmount()) > 0) {
            discountAmount = appliedDiscount.getMaxDiscountAmount();
        }

        appliedDiscount.setUsedCount(appliedDiscount.getUsedCount() + 1);
        discountRepository.save(appliedDiscount);

        return new DiscountResult(discountAmount, appliedDiscount);
    }

    private Order saveOrder(User user, Address address, ShippingCarrier carrier, Discount discount,
            BigDecimal subtotal, BigDecimal discountAmount, BigDecimal shippingFee,
            BigDecimal grandTotal, PaymentMethod paymentMethod, String note) {
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
                .discount(discount)
                .note(note)
                .paymentMethod(paymentMethod)
                .build();

        return orderRepository.save(order);
    }

    private void saveOrderItems(Order order, List<CartItem> items) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : items) {
            Product product = item.getProduct();
            BigDecimal currentPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productImage(product.getImageUrl())
                    .unitPrice(currentPrice)
                    .quantity(item.getQuantity())
                    .subtotal(currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                    .seller(product.getSeller())
                    .sellerName(product.getSeller().getFullName())
                    .build();
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);
    }

    private Payment createPayment(Order order, PaymentMethod method, BigDecimal amount) {
        Payment payment = Payment.builder()
                .order(order)
                .method(method)
                .status(PaymentStatus.UNPAID)
                .amount(amount)
                .build();
        return paymentRepository.save(payment);
    }

    private String generatePaymentUrl(String orderCode, PaymentMethod method) {
        if (method != PaymentMethod.COD) {
            return "https://payment.sandbox.com/pay/" + orderCode;
        }
        return null;
    }

    private record DiscountResult(BigDecimal amount, Discount discount) {
    }
}
