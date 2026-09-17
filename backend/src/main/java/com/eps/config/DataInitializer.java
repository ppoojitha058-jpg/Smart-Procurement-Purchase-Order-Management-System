package com.eps.config;

import com.eps.entity.*;
import com.eps.enums.ProductStatus;
import com.eps.enums.SupplierStatus;
import com.eps.repository.*;
import com.eps.service.RoleService;
import com.eps.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Application data initializer
 * Seeds roles, users for each role, departments, categories, suppliers, and catalog items.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleService roleService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting EPS Data Initializer...");

        // 1. Roles
        Role adminRole = getOrCreateRole("ADMIN");
        Role userRole = getOrCreateRole("USER");
        Role managerRole = getOrCreateRole("MANAGER");
        Role procurementRole = getOrCreateRole("PROCUREMENT");
        Role supplierRole = getOrCreateRole("SUPPLIER");
        Role receivingRole = getOrCreateRole("RECEIVING");
        Role financeRole = getOrCreateRole("FINANCE");

        // 2. Users for each role
        User admin = getOrCreateUser("admin@eps.com", "Admin@123", "System Administrator", "1000000000", "HQ Office", adminRole);
        User employee = getOrCreateUser("employee@eps.com", "User@123", "John Doe (Employee)", "1000000001", "Floor 3, IT Dept", userRole);
        User manager = getOrCreateUser("manager@eps.com", "Manager@123", "Sarah Jenkins (Operations Manager)", "1000000002", "Floor 4, Management", managerRole);
        User procurement = getOrCreateUser("procurement@eps.com", "Procurement@123", "Alex Morgan (Procurement Lead)", "1000000003", "Procurement Bay", procurementRole);
        User supplier = getOrCreateUser("supplier@eps.com", "Supplier@123", "Acme Industrial Supplies", "1000000004", "Tech Park, Suite 10", supplierRole);
        User receiving = getOrCreateUser("receiving@eps.com", "Receiving@123", "Marcus Vance (Receiving Officer)", "1000000005", "Warehouse Bay 4", receivingRole);
        User finance = getOrCreateUser("finance@eps.com", "Finance@123", "Elena Rostova (Finance Director)", "1000000006", "Finance Tower", financeRole);

        // 3. Departments
        Department itDept = departmentRepository.findByDepartmentName("Information Technology")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Information Technology", "Sarah Jenkins")));
        Department opsDept = departmentRepository.findByDepartmentName("Operations & Facilities")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Operations & Facilities", "Sarah Jenkins")));
        Department financeDept = departmentRepository.findByDepartmentName("Finance & Accounting")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Finance & Accounting", "Elena Rostova")));
        Department hrDept = departmentRepository.findByDepartmentName("Human Resources")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Human Resources", "Lisa Hammond")));
        Department marketingDept = departmentRepository.findByDepartmentName("Marketing & Communications")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Marketing & Communications", "Kevin Torres")));
        Department engineeringDept = departmentRepository.findByDepartmentName("Engineering")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Engineering", "Raj Patel")));

        if (employee.getDepartment() == null) {
            employee.setDepartment(itDept);
            userService.saveUser(employee);
        }
        // Sarah Jenkins is the configured manager for Information Technology.
        // Keep the authenticated manager's department aligned with that routing
        // configuration so IT requisitions reach the correct approval queue.
        if (manager.getDepartment() == null || !itDept.equals(manager.getDepartment())) {
            manager.setDepartment(itDept);
            userService.saveUser(manager);
        }

        // 4. Categories (8 corporate categories)
        Category hardwareCat = categoryRepository.findByCategoryName("IT Hardware & Computing")
                .orElseGet(() -> categoryRepository.save(new Category(null, "IT Hardware & Computing")));
        Category furnitureCat = categoryRepository.findByCategoryName("Office Furniture & Ergonomics")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Office Furniture & Ergonomics")));
        Category suppliesCat = categoryRepository.findByCategoryName("General Supplies & Equipment")
                .orElseGet(() -> categoryRepository.save(new Category(null, "General Supplies & Equipment")));
        Category networkCat = categoryRepository.findByCategoryName("Network Infrastructure")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Network Infrastructure")));
        Category softwareCat = categoryRepository.findByCategoryName("Software Licenses")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Software Licenses")));
        Category safetyCat = categoryRepository.findByCategoryName("Safety & Security Gear")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Safety & Security Gear")));
        Category printingCat = categoryRepository.findByCategoryName("Printing & Imaging")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Printing & Imaging")));
        Category communicationCat = categoryRepository.findByCategoryName("Communication Devices")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Communication Devices")));

        // 5. Suppliers
        Supplier defaultSupplier = supplierRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Supplier s = new Supplier();
                    s.setName("Apex Technologies Global");
                    s.setEmail("supplier@eps.com");
                    s.setPhone("+1-800-555-0199");
                    s.setAddress("100 Enterprise Way, Silicon Valley");
                    s.setGstNumber("GSTIN29AAAAA0000A1Z5");
                    s.setStatus(SupplierStatus.ACTIVE);
                    s.setRating(new BigDecimal("4.8"));
                    s.setFeedback("Preferred tier-1 enterprise supplier");
                    return supplierRepository.save(s);
                });

        // 6. Products (10 products across 8 categories - upsert so SKUs and images are populated)
        upsertProduct("Dell Latitude 5540 Enterprise Laptop", "SKU-HW-0011",
                "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400",
                "15.6-inch FHD, Intel Core i7, 16GB RAM, 512GB SSD Enterprise Laptop",
                new BigDecimal("1250.00"), 50, itDept, hardwareCat, admin);

        upsertProduct("Herman Miller Aeron Ergonomic Chair", "SKU-FN-0021",
                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400",
                "Fully adjustable posture fit, lumbar support, breathable mesh task chair",
                new BigDecimal("480.00"), 100, opsDept, furnitureCat, admin);

        upsertProduct("Dell UltraSharp 32\" 4K USB-C Hub Monitor", "SKU-HW-0031",
                "https://images.unsplash.com/photo-1527443224154-c4a573d5778c?w=400",
                "IPS Black technology, 90W power delivery, HDR 400 professional display",
                new BigDecimal("690.00"), 75, itDept, hardwareCat, admin);

        upsertProduct("Logitech MX Master 3S Mouse & Keyboard Bundle", "SKU-GS-0041",
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400",
                "Quiet clicks, 8K DPI sensor, tactile quiet mechanical switches",
                new BigDecimal("210.00"), 120, itDept, suppliesCat, admin);

        upsertProduct("Cisco Catalyst 2960X 24-Port PoE+ Network Switch", "SKU-NW-0051",
                "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400",
                "24 x 10/100/1000 Ethernet PoE+ ports, 370W PoE budget, enterprise-grade managed switch",
                new BigDecimal("2150.00"), 15, itDept, networkCat, admin);

        upsertProduct("Microsoft 365 Business Premium – 1 Year License", "SKU-SW-0061",
                "https://images.unsplash.com/photo-1633419461186-7d40a38105ec?w=400",
                "Full Office apps, 1TB OneDrive, Teams, Exchange, Azure AD P1, Intune device management",
                new BigDecimal("264.00"), 200, itDept, softwareCat, admin);

        upsertProduct("Axis P3245-V Fixed Dome IP Security Camera", "SKU-SF-0071",
                "https://images.unsplash.com/photo-1557804506-669a67965ba0?w=400",
                "2MP HDTV 1080p, Wide Dynamic Range, IR illumination, vandal-resistant dome",
                new BigDecimal("385.00"), 30, opsDept, safetyCat, admin);

        upsertProduct("HP LaserJet Pro MFP M428fdw Enterprise Printer", "SKU-PR-0081",
                "https://images.unsplash.com/photo-1612198188060-c7c2a3b66eae?w=400",
                "40 ppm mono laser, auto duplex print/scan/copy/fax, built-in Wi-Fi & Ethernet",
                new BigDecimal("549.00"), 20, opsDept, printingCat, admin);

        upsertProduct("Poly Sync 60 USB Bluetooth Smart Speakerphone", "SKU-CD-0091",
                "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=400",
                "360° voice pickup, AI noise elimination, USB-A/USB-C/BT, conference room ready",
                new BigDecimal("319.00"), 40, hrDept, communicationCat, admin);

        upsertProduct("Steelcase Flex Electric Height-Adjustable Standing Desk", "SKU-FN-0101",
                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=400",
                "Electric dual-motor lift, 24\"-50\" height range, cable management, 80kg capacity",
                new BigDecimal("895.00"), 60, opsDept, furnitureCat, admin);

        upsertProduct("Apple MacBook Pro 16\" M3 Pro 36GB/512GB", "SKU-HW-0111",
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400",
                "Space Black, 12-core CPU, 18-core GPU, Liquid Retina XDR display, macOS Sonoma",
                new BigDecimal("2499.00"), 35, itDept, hardwareCat, admin);

        upsertProduct("Lenovo ThinkPad X1 Carbon Gen 11 Ultrabook", "SKU-HW-0121",
                "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=400",
                "14-inch 2.8K OLED, Intel Core i7-1365U, 32GB LPDDR5, 1TB NVMe, Carbon Fiber chassis",
                new BigDecimal("1850.00"), 40, engineeringDept, hardwareCat, admin);

        upsertProduct("Samsung 49\" Odyssey Neo G9 Dual QHD Curved Monitor", "SKU-HW-0131",
                "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400",
                "Quantum Mini-LED, 240Hz, 1ms, 1000R curvature, dual QHD 5120x1440 resolution",
                new BigDecimal("1399.00"), 20, engineeringDept, hardwareCat, admin);

        upsertProduct("Ubiquiti UniFi Dream Machine Special Edition (UDM-SE)", "SKU-NW-0141",
                "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=400",
                "Enterprise 10G Security Gateway, 8-Port PoE switch, 128GB SSD, UniFi OS controller",
                new BigDecimal("499.00"), 25, itDept, networkCat, admin);

        upsertProduct("APC Smart-UPS 1500VA LCD RM 2U 120V with SmartConnect", "SKU-HW-0151",
                "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400",
                "1000 Watts / 1440 VA, 6x NEMA 5-15R battery backup, pure sine wave power protection",
                new BigDecimal("780.00"), 18, itDept, hardwareCat, admin);

        upsertProduct("Sony WH-1000XM5 ANC Enterprise Headset", "SKU-CD-0161",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
                "Auto NC Optimizer, 8 microphones, 30-hour battery, crystal-clear beamforming calls",
                new BigDecimal("399.00"), 80, hrDept, communicationCat, admin);

        upsertProduct("Synology DiskStation DS923+ 4-Bay NAS Server", "SKU-NW-0171",
                "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=400",
                "Dual M.2 NVMe SSD cache slots, 10GbE network expansion, AMD Ryzen R1600 processor",
                new BigDecimal("599.00"), 15, itDept, networkCat, admin);

        upsertProduct("First Aid & Workplace Trauma Response Station", "SKU-SF-0181",
                "https://images.unsplash.com/photo-1603398938378-e54eab446dde?w=400",
                "OSHA/ANSI 2021 Class B compliant, 4-shelf industrial steel cabinet, eye wash station",
                new BigDecimal("275.00"), 50, opsDept, safetyCat, admin);

        upsertProduct("Brother P-Touch PT-D600 Industrial Label Maker", "SKU-PR-0191",
                "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=400",
                "Full-color graphic display, PC/Mac USB connectivity, high-res laminated labels",
                new BigDecimal("145.00"), 65, opsDept, printingCat, admin);

        upsertProduct("Adobe Creative Cloud All Apps Enterprise – 1 Year", "SKU-SW-0201",
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400",
                "Photoshop, Illustrator, Premiere Pro, Acrobat DC, 100GB cloud storage, 24/7 support",
                new BigDecimal("1198.00"), 50, itDept, softwareCat, admin);

        log.info("Initialized/Updated 20 enterprise catalog products across 8 categories");

        log.info("EPS Data Initializer finished successfully!");
    }

    private void upsertProduct(String name, String sku, String imageUrl, String description, BigDecimal price, int qty, Department dept, Category cat, User user) {
        java.util.Optional<Supplier> defaultSupplierOpt = supplierRepository.findByEmailIgnoreCase("supplier@eps.com")
                .or(() -> supplierRepository.findAll().stream().findFirst());
        upsertProductWithSupplier(name, sku, imageUrl, description, price, qty, dept, cat, user, defaultSupplierOpt.orElse(null));
    }

    private void upsertProductWithSupplier(String name, String sku, String imageUrl, String description, BigDecimal price, int qty, Department dept, Category cat, User user, Supplier supplier) {
        Product p = productRepository.findBySku(sku)
                .or(() -> productRepository.findByName(name))
                .or(() -> productRepository.findAll().stream()
                        .filter(x -> x.getName() != null && (
                                (name.contains("Latitude") && x.getName().contains("Latitude")) ||
                                (name.contains("Herman") && x.getName().contains("Herman")) ||
                                (name.contains("UltraSharp") && x.getName().contains("UltraSharp")) ||
                                (name.contains("Logitech") && x.getName().contains("Logitech"))
                        ))
                        .findFirst())
                .orElse(new Product());
        p.setName(name);
        p.setSku(sku);
        p.setImageUrl(imageUrl);
        p.setDescription(description);
        p.setPricePerProduct(price);
        p.setNumberOfQuantities(qty);
        p.setDepartment(dept);
        p.setCategory(cat);
        p.setStatus(ProductStatus.ACTIVE);
        p.setUser(user);
        if (p.getSupplier() == null && supplier != null) {
            p.setSupplier(supplier);
        }
        productRepository.save(p);
        // Backfill any legacy products with no supplier so supplier catalog is visible
        try {
            java.util.List<Product> orphans = productRepository.findAll().stream().filter(x -> x.getSupplier() == null).toList();
            for (Product o : orphans) {
                o.setSupplier(supplier);
                productRepository.save(o);
            }
        } catch (Exception ex) {
            log.warn("Supplier backfill skipped: {}", ex.getMessage());
        }
    }

    private Role getOrCreateRole(String roleName) {
        return roleService.getRoleByName(roleName)
                .orElseGet(() -> roleService.saveRole(new Role(null, roleName)));
    }

    private User getOrCreateUser(String email, String rawPassword, String fullName, String phone, String address, Role role) {
        return userService.getUserByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setPassword(passwordEncoder.encode(rawPassword));
                    u.setFullName(fullName);
                    u.setPhone(phone);
                    u.setAddress(address);
                    u.setRole(role);
                    u.setEnabled(true);
                    User saved = userService.saveUser(u);
                    log.info("Created account: {} ({})", email, role.getRoleName());
                    return saved;
                });
    }
}
