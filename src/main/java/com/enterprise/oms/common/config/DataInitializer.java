package com.enterprise.oms.common.config;

import com.enterprise.oms.inventory.domain.Inventory;
import com.enterprise.oms.inventory.domain.Warehouse;
import com.enterprise.oms.inventory.repository.InventoryRepository;
import com.enterprise.oms.inventory.repository.WarehouseRepository;
import com.enterprise.oms.product.domain.Category;
import com.enterprise.oms.product.domain.Product;
import com.enterprise.oms.product.repository.CategoryRepository;
import com.enterprise.oms.product.repository.ProductRepository;
import com.enterprise.oms.user.domain.Role;
import com.enterprise.oms.user.domain.User;
import com.enterprise.oms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data generation.");
            return;
        }

        log.info("Seeding initial Enterprise OMS data...");

        // 1. Seed Users
        User admin = userRepository.save(User.builder()
                .email("admin@enterprise.com")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .role(Role.ADMIN)
                .build());

        User customer = userRepository.save(User.builder()
                .email("customer@enterprise.com")
                .password(passwordEncoder.encode("customer123"))
                .fullName("John Doe (Customer)")
                .role(Role.CUSTOMER)
                .build());

        User warehouseManager = userRepository.save(User.builder()
                .email("warehouse@enterprise.com")
                .password(passwordEncoder.encode("manager123"))
                .fullName("Sarah Connor (Warehouse Manager)")
                .role(Role.WAREHOUSE_MANAGER)
                .build());

        User supportAgent = userRepository.save(User.builder()
                .email("support@enterprise.com")
                .password(passwordEncoder.encode("support123"))
                .fullName("Alex Smith (Support Agent)")
                .role(Role.SUPPORT_AGENT)
                .build());

        // 2. Seed Categories
        Category electronics = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Gadgets, Laptops, Mobile Devices and Accessories")
                .build());

        Category furniture = categoryRepository.save(Category.builder()
                .name("Office Furniture")
                .description("Ergonomic chairs, standing desks, monitors")
                .build());

        // 3. Seed Products
        Product macbook = productRepository.save(Product.builder()
                .sku("LAP-MAC-M3-16")
                .name("Apple MacBook Pro 16\" (M3 Max)")
                .description("16-inch Retina XDR display, 36GB RAM, 1TB SSD")
                .price(new BigDecimal("2499.00"))
                .category(electronics)
                .build());

        Product phone = productRepository.save(Product.builder()
                .sku("MOB-PIX-8PRO")
                .name("Google Pixel 8 Pro 256GB")
                .description("Pro camera system, Tensor G3 chip, 120Hz LTPO display")
                .price(new BigDecimal("999.00"))
                .category(electronics)
                .build());

        Product chair = productRepository.save(Product.builder()
                .sku("FUR-HER-AERON")
                .name("Herman Miller Aeron Ergonomic Chair")
                .description("Fully adjustable posturefit SL support, mineral mesh")
                .price(new BigDecimal("1495.00"))
                .category(furniture)
                .build());

        Product monitor = productRepository.save(Product.builder()
                .sku("DIS-DELL-U2724D")
                .name("Dell UltraSharp 27\" 4K USB-C Hub Monitor")
                .description("IPS Black display, 98% DCI-P3 color gamut, 90W Power Delivery")
                .price(new BigDecimal("579.00"))
                .category(electronics)
                .build());

        // 4. Seed Warehouses
        Warehouse whEast = warehouseRepository.save(Warehouse.builder()
                .code("WH-US-EAST")
                .name("New York Fulfillment Center")
                .location("Newark, NJ")
                .build());

        Warehouse whWest = warehouseRepository.save(Warehouse.builder()
                .code("WH-US-WEST")
                .name("Silicon Valley Logistics Hub")
                .location("San Jose, CA")
                .build());

        // 5. Seed Inventory
        inventoryRepository.save(Inventory.builder()
                .product(macbook)
                .warehouse(whEast)
                .quantityAvailable(25)
                .quantityReserved(0)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(phone)
                .warehouse(whEast)
                .quantityAvailable(50)
                .quantityReserved(0)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(chair)
                .warehouse(whWest)
                .quantityAvailable(5) // Low stock product for dashboard demonstration!
                .quantityReserved(0)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(monitor)
                .warehouse(whWest)
                .quantityAvailable(40)
                .quantityReserved(0)
                .build());

        log.info("Data initialization complete! Default accounts created:");
        log.info("  Admin: admin@enterprise.com / admin123");
        log.info("  Customer: customer@enterprise.com / customer123");
        log.info("  Warehouse Manager: warehouse@enterprise.com / manager123");
        log.info("  Support Agent: support@enterprise.com / support123");
    }
}
