package com.example.tracking_order.config;

import com.example.tracking_order.modules.catalog.entity.Category;
import com.example.tracking_order.modules.catalog.entity.Inventory;
import com.example.tracking_order.modules.catalog.entity.Product;
import com.example.tracking_order.modules.catalog.enums.ProductStatus;
import com.example.tracking_order.modules.catalog.repository.CategoryRepository;
import com.example.tracking_order.modules.catalog.repository.InventoryRepository;
import com.example.tracking_order.modules.catalog.repository.ProductRepository;
import com.example.tracking_order.modules.shipping.entity.ShippingCarrier;
import com.example.tracking_order.modules.shipping.repository.ShippingCarrierRepository;
import com.example.tracking_order.modules.user.entity.User;
import com.example.tracking_order.modules.user.enums.Role;
import com.example.tracking_order.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ShippingCarrierRepository shippingCarrierRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedData();
        }
    }

    private void seedData() {
        // 1. Seed Users
        String encodedPassword = passwordEncoder.encode("password123");
        
        User admin = User.builder()
                .fullName("Admin System")
                .email("admin@example.com")
                .passwordHash(encodedPassword)
                .phone("0123456789")
                .roles(Set.of(Role.ADMIN))
                .build();
        userRepository.save(admin);

        User seller = User.builder()
                .fullName("Shop Online")
                .email("seller@example.com")
                .passwordHash(encodedPassword)
                .phone("0987654321")
                .roles(Set.of(Role.SELLER))
                .build();
        userRepository.save(seller);

        User customer = User.builder()
                .fullName("Nguyễn Văn A")
                .email("customer@example.com")
                .passwordHash(encodedPassword)
                .phone("0333444555")
                .roles(Set.of(Role.BUYER))
                .build();
        userRepository.save(customer);

        // 2. Seed Categories
        List<Category> categories = new ArrayList<>();
        categories.add(Category.builder().name("Điện tử").slug("dien-tu").build());
        categories.add(Category.builder().name("Thời trang").slug("thoi-trang").build());
        categories.add(Category.builder().name("Gia dụng").slug("gia-dung").build());
        categories.add(Category.builder().name("Sách").slug("sach").build());
        categoryRepository.saveAll(categories);

        // 3. Seed Shipping Carriers
        List<ShippingCarrier> carriers = new ArrayList<>();
        carriers.add(ShippingCarrier.builder().code("GHTK").name("Giao Hàng Tiết Kiệm").region("Toàn quốc").baseFee(new BigDecimal("20000")).build());
        carriers.add(ShippingCarrier.builder().code("GHN").name("Giao Hàng Nhanh").region("Toàn quốc").baseFee(new BigDecimal("25000")).build());
        carriers.add(ShippingCarrier.builder().code("VNP").name("VNPost").region("Toàn quốc").baseFee(new BigDecimal("15000")).build());
        shippingCarrierRepository.saveAll(carriers);

        // 4. Seed Products and Inventory
        Category electronics = categories.get(0);
        Category fashion = categories.get(1);

        for (int i = 1; i <= 10; i++) {
            Product p = Product.builder()
                    .seller(seller)
                    .category(i % 2 == 0 ? electronics : fashion)
                    .name("Sản phẩm mẫu " + i)
                    .sku("SKU-00" + i)
                    .slug("san-pham-mau-" + i)
                    .description("Mô tả cho sản phẩm mẫu số " + i)
                    .basePrice(new BigDecimal(100000 + i * 50000))
                    .salePrice(new BigDecimal(90000 + i * 50000))
                    .status(ProductStatus.ACTIVE)
                    .build();
            productRepository.save(p);

            Inventory inv = Inventory.builder()
                    .product(p)
                    .quantityInStock(100)
                    .quantityReserved(0)
                    .build();
            inventoryRepository.save(inv);
        }
    }
}
