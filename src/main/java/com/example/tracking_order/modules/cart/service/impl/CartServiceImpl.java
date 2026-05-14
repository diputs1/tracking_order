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
import com.example.tracking_order.modules.catalog.entity.Inventory;
import com.example.tracking_order.modules.catalog.entity.Product;
import com.example.tracking_order.modules.catalog.enums.ProductStatus;
import com.example.tracking_order.modules.catalog.repository.InventoryRepository;
import com.example.tracking_order.modules.catalog.repository.ProductRepository;
import com.example.tracking_order.modules.user.entity.User;
import com.example.tracking_order.modules.user.repository.UserRepository;
import com.example.tracking_order.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final CartMapper cartMapper;

    @Autowired
    @Lazy
    private CartService self;

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
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public CartDto getCart() {
        return getCartInternal();
    }

    private CartDto getCartInternal() {
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
    @Transactional(rollbackFor = Exception.class)
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
            existingItem
                    .setPriceSnapshot(product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice());
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

        return self.getCart();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartDto updateCartItem(Long itemId, UpdateCartItemRequest request) {
        User user = getCurrentUser();
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN,
                        "Bạn không có quyền thực hiện hành động này hoặc sản phẩm không có trong giỏ"));

        if (request.getQuantity() < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Số lượng sản phẩm không được nhỏ hơn 0");
        }

        if (request.getQuantity() == 0) {
            cartItemRepository.delete(item);
            return self.getCart();
        }

        Inventory inventory = inventoryRepository.findByProductIdWithLock(item.getProduct().getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không có thông tin tồn kho"));

        int available = inventory.getQuantityInStock() - inventory.getQuantityReserved();
        if (request.getQuantity() > available) {
            throw new AppException(ErrorCode.OUT_OF_STOCK, "Tồn kho không đủ");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return self.getCart();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartDto removeCartItem(Long itemId) {
        User user = getCurrentUser();
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN,
                        "Bạn không có quyền thực hiện hành động này hoặc sản phẩm không có trong giỏ"));
        cartItemRepository.delete(item);
        return self.getCart();
    }
}
