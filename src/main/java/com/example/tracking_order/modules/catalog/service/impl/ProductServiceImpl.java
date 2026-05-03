package com.example.tracking_order.modules.catalog.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.catalog.dto.*;
import com.example.tracking_order.modules.catalog.entity.Category;
import com.example.tracking_order.modules.catalog.entity.Inventory;
import com.example.tracking_order.modules.catalog.entity.Product;
import com.example.tracking_order.modules.catalog.enums.ProductStatus;
import com.example.tracking_order.modules.catalog.repository.CategoryRepository;
import com.example.tracking_order.modules.catalog.repository.InventoryRepository;
import com.example.tracking_order.modules.catalog.repository.ProductRepository;
import com.example.tracking_order.modules.catalog.service.ProductService;
import com.example.tracking_order.modules.user.entity.User;
import com.example.tracking_order.modules.user.repository.UserRepository;
import com.example.tracking_order.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @Override
    public PageData<ProductListDto> getProducts(String search, String sku, Long categoryId, ProductStatus status,
                                                 BigDecimal minPrice, BigDecimal maxPrice, Long sellerId,
                                                 String sort, int page, int size) {
        
        Specification<Product> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            
            if (search != null && !search.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            if (sku != null && !sku.isEmpty()) {
                predicates.add(cb.equal(root.get("sku"), sku));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Sort sortOrder = Sort.unsorted();
        if ("price_asc".equals(sort)) sortOrder = Sort.by("basePrice").ascending();
        else if ("price_desc".equals(sort)) sortOrder = Sort.by("basePrice").descending();
        else if ("newest".equals(sort)) sortOrder = Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page - 1, size, sortOrder);
        Page<com.example.tracking_order.modules.catalog.dto.ProductProjection> productPage = productRepository.findAllProjected(spec, pageable);

        List<ProductListDto> items = productPage.getContent().stream()
                .map(this::mapProjectionToDto)
                .collect(Collectors.toList());

        PageData.Pagination pagination = PageData.Pagination.builder()
                .page(page)
                .total_pages(productPage.getTotalPages())
                .total_items(productPage.getTotalElements())
                .build();

        return PageData.<ProductListDto>builder()
                .items(items)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public ProductDetailDto createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new AppException(ErrorCode.DUPLICATE_SKU, "Mã SKU đã tồn tại");
        }

        Category category = categoryRepository.findById(request.getCategory_id())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Danh mục không tồn tại"));

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User seller = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Người bán không hợp lệ"));

        String slug = toSlug(request.getName()) + "-" + System.currentTimeMillis();

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .slug(slug)
                .basePrice(request.getBase_price())
                .salePrice(request.getSale_price())
                .description(request.getDescription())
                .weight(request.getWeight() != null ? request.getWeight() : BigDecimal.ZERO)
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                .category(category)
                .seller(seller)
                .build();
        
        product = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(product)
                .quantityInStock(request.getInitial_stock())
                .quantityReserved(0)
                .build();
        inventoryRepository.save(inventory);

        return getProductDetail(product.getId());
    }

    @Override
    public ProductDetailDto getProductDetail(Long productId) {
        Product product = productRepository.findByIdWithDetails(productId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không tồn tại"));
        return mapToDetailDto(product);
    }

    @Override
    @Transactional
    public ProductDetailDto updateProduct(Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không tồn tại"));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getBase_price() != null) product.setBasePrice(request.getBase_price());
        if (request.getSale_price() != null) product.setSalePrice(request.getSale_price());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getWeight() != null) product.setWeight(request.getWeight());

        productRepository.save(product);
        return getProductDetail(productId);
    }

    @Override
    public InventoryDto getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không có kho"));
        return mapToInventoryDto(inventory);
    }

    @Override
    @Transactional
    public InventoryDto updateInventory(Long productId, UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không có kho"));

        inventory.setQuantityInStock(request.getQuantity_in_stock());
        inventoryRepository.save(inventory);

        return mapToInventoryDto(inventory);
    }

    private ProductListDto mapProjectionToDto(com.example.tracking_order.modules.catalog.dto.ProductProjection projection) {
        int inStock = projection.getQuantityInStock() != null ? projection.getQuantityInStock() : 0;
        int reserved = projection.getQuantityReserved() != null ? projection.getQuantityReserved() : 0;

        return ProductListDto.builder()
                .id(projection.getId())
                .name(projection.getName())
                .sku(projection.getSku())
                .slug(projection.getSlug())
                .base_price(projection.getBasePrice())
                .sale_price(projection.getSalePrice())
                .status(projection.getStatus())
                .category(ProductListDto.CategoryRef.builder()
                        .id(projection.getCategoryId())
                        .name(projection.getCategoryName())
                        .build())
                .seller(ProductListDto.SellerRef.builder()
                        .id(projection.getSellerId())
                        .name(projection.getSellerFullName())
                        .build())
                .inventory(ProductListDto.InventoryRef.builder()
                        .quantity_in_stock(inStock)
                        .quantity_available(inStock - reserved)
                        .build())
                .rating_avg(BigDecimal.ZERO)
                .build();
    }

    private ProductListDto mapToListDto(Product product) {
        Inventory inventory = product.getInventory();
        int inStock = inventory != null ? inventory.getQuantityInStock() : 0;
        int reserved = inventory != null ? inventory.getQuantityReserved() : 0;

        return ProductListDto.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .slug(product.getSlug())
                .base_price(product.getBasePrice())
                .sale_price(product.getSalePrice())
                .status(product.getStatus().name())
                .category(ProductListDto.CategoryRef.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .build())
                .seller(ProductListDto.SellerRef.builder()
                        .id(product.getSeller().getId())
                        .name(product.getSeller().getFullName())
                        .build())
                .inventory(ProductListDto.InventoryRef.builder()
                        .quantity_in_stock(inStock)
                        .quantity_available(inStock - reserved)
                        .build())
                .rating_avg(BigDecimal.ZERO)
                .build();
    }

    private ProductDetailDto mapToDetailDto(Product product) {
        Inventory inventory = product.getInventory();
        int inStock = inventory != null ? inventory.getQuantityInStock() : 0;
        int reserved = inventory != null ? inventory.getQuantityReserved() : 0;

        return ProductDetailDto.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .slug(product.getSlug())
                .description(product.getDescription())
                .base_price(product.getBasePrice())
                .sale_price(product.getSalePrice())
                .weight(product.getWeight())
                .status(product.getStatus().name())
                .category(ProductDetailDto.CategoryRef.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .slug(product.getCategory().getSlug())
                        .build())
                .seller(ProductDetailDto.SellerRef.builder()
                        .id(product.getSeller().getId())
                        .name(product.getSeller().getFullName())
                        .build())
                .inventory(ProductDetailDto.InventoryRef.builder()
                        .quantity_in_stock(inStock)
                        .quantity_available(inStock - reserved)
                        .build())
                .rating_avg(BigDecimal.ZERO)
                .review_count(0)
                .build();
    }

    private InventoryDto mapToInventoryDto(Inventory inventory) {
        int inStock = inventory.getQuantityInStock();
        int reserved = inventory.getQuantityReserved();
        int available = inStock - reserved;

        return InventoryDto.builder()
                .product_id(inventory.getProduct().getId())
                .quantity_in_stock(inStock)
                .quantity_reserved(reserved)
                .quantity_available(available)
                .is_available(available > 0)
                .updated_at(inventory.getUpdatedAt())
                .build();
    }

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");

    public static String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITE_SPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
