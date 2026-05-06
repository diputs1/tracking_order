package com.example.tracking_order.modules.cart.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.modules.cart.dto.*;
import com.example.tracking_order.modules.cart.entity.Cart;
import com.example.tracking_order.modules.cart.entity.CartItem;
import com.example.tracking_order.modules.cart.mapper.CartMapper;
import com.example.tracking_order.modules.cart.repository.CartItemRepository;
import com.example.tracking_order.modules.cart.repository.CartRepository;
import com.example.tracking_order.modules.cart.service.CartService;
import com.example.tracking_order.modules.order.mapper.OrderMapper;
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
import java.util.HashMap;
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
    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;

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
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        
        if (cartOpt.isEmpty()) {
            return CartDto.builder()
                    .items(new ArrayList<>())
                    .summary(CartDto.Summary.builder()
                            .itemCount(0)
                            .totalQty(0)
                            .subtotal(BigDecimal.ZERO)
                            .hasOutOfStock(false)
                            .build())
                    .build();
        }
        
        Cart cart = cartOpt.get();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartDto.CartItemDto> itemDtos = new ArrayList<>();
        
        int totalQty = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasOutOfStock = false;
        
        List<Long> idProduct = items.stream()
                .map(item -> item.getProduct().getId())
                .toList();
                
        List<Inventory> listInventory = inventoryRepository.findByProductIdIn(idProduct);
        HashMap<Long, Inventory> productIdInventoryMap = new HashMap<>();
        for (Inventory inventory : listInventory) {
            productIdInventoryMap.put(inventory.getProduct().getId(), inventory);
        }

        for (CartItem item : items) {
            Product product = item.getProduct();
            Inventory inventory = productIdInventoryMap.get(product.getId());
            
            CartDto.CartItemDto itemDto = buildCartItemDto(item, inventory);
            
            if (!itemDto.getIsAvailable()) {
                hasOutOfStock = true;
            }
            
            totalQty += item.getQuantity();
            if (itemDto.getIsAvailable()) {
                subtotal = subtotal.add(itemDto.getSubtotal());
            }
            
            itemDtos.add(itemDto);
        }

        CartDto.Summary summary = CartDto.Summary.builder()
                .itemCount(items.size())
                .totalQty(totalQty)
                .subtotal(subtotal)
                .hasOutOfStock(hasOutOfStock)
                .build();

        CartDto cartDto = cartMapper.toCartDto(cart);
        cartDto.setItems(itemDtos);
        cartDto.setSummary(summary);
        return cartDto;
    }

    private CartDto.CartItemDto buildCartItemDto(CartItem item, Inventory inventory) {
        Product product = item.getProduct();
        
        int inStock = inventory != null ? inventory.getQuantityInStock() - inventory.getQuantityReserved() : 0;
        boolean isAvailable = inStock >= item.getQuantity() && product.getStatus() == ProductStatus.ACTIVE;
        
        BigDecimal currentPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();
        BigDecimal itemSubtotal = currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        
        return cartMapper.toCartItemDto(item, currentPrice, inStock, isAvailable, itemSubtotal);
    }

    @Override
    @Transactional
    public CartDto addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        if (request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Số lượng sản phẩm phải lớn hơn 0");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không tồn tại"));
                
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Sản phẩm ngừng kinh doanh");
        }
        
        Inventory inventory = inventoryRepository.findByProductIdWithLock(product.getId())
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
        User user = getCurrentUser();
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện hành động này hoặc sản phẩm không có trong giỏ"));
                
        if (request.getQuantity() < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Số lượng sản phẩm không được nhỏ hơn 0");
        }

        if (request.getQuantity() == 0) {
            cartItemRepository.delete(item);
            return getCart();
        }
        
        Inventory inventory = inventoryRepository.findByProductIdWithLock(item.getProduct().getId())
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
        User user = getCurrentUser();
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện hành động này hoặc sản phẩm không có trong giỏ"));
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
        
        return processCheckout(user, cartItems, request.getAddressId(), request.getCarrierId(), 
                request.getPaymentMethod(), request.getDiscountCode(), request.getNote(), cart.getId());
    }
    
    @Override
    @Transactional
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
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Sản phẩm " + product.getName() + " ngừng kinh doanh");
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

        Discount appliedDiscount = discountRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Mã giảm giá không hợp lệ"));

        LocalDateTime now = LocalDateTime.now();
        if (appliedDiscount.getStartAt().isAfter(now) || appliedDiscount.getExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Mã giảm giá đã hết hạn hoặc chưa áp dụng");
        }
        if (subtotal.compareTo(appliedDiscount.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã");
        }

        BigDecimal discountAmount;
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

    private record DiscountResult(BigDecimal amount, Discount discount) {}
}
