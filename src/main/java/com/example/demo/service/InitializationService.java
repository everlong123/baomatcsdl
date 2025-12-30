package com.example.demo.service;

import com.example.demo.model.AppLoginUser;
import com.example.demo.repository.AppLoginUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Service để khởi tạo user SEC_ADMIN vào APP_LOGIN_USER table.
 * Chạy tự động khi ứng dụng start nếu SEC_ADMIN chưa tồn tại trong APP_LOGIN_USER.
 * 
 * Lưu ý: SEC_ADMIN đã được tạo sẵn trong Oracle bằng SYS.
 * Service này chỉ thêm vào APP_LOGIN_USER để có thể login qua ứng dụng.
 */
@Service
@Order(1)
public class InitializationService implements CommandLineRunner {
    
    @Autowired
    private AppLoginUserRepository appLoginUserRepository;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // Kiểm tra xem SEC_ADMIN đã có trong APP_LOGIN_USER chưa
            AppLoginUser existingUser = appLoginUserRepository.findByUsername("SEC_ADMIN");
            
            if (existingUser == null) {
                System.out.println("========================================");
                System.out.println("Đang khởi tạo SEC_ADMIN trong APP_LOGIN_USER...");
                
                // Hash password "admin123" bằng BCrypt
                String passwordHash = authenticationService.encodePassword("admin123");
                System.out.println("Password đã được hash: " + passwordHash.substring(0, Math.min(20, passwordHash.length())) + "...");
                
                // Insert vào APP_LOGIN_USER
                AppLoginUser secAdmin = new AppLoginUser("SEC_ADMIN", passwordHash);
                appLoginUserRepository.save(secAdmin);
                
                // Verify insertion
                AppLoginUser verifyUser = appLoginUserRepository.findByUsername("SEC_ADMIN");
                if (verifyUser != null) {
                    System.out.println("========================================");
                    System.out.println("SUCCESS: SEC_ADMIN đã được thêm vào APP_LOGIN_USER");
                    System.out.println("Username: SEC_ADMIN");
                    System.out.println("Password: admin123 (đã hash bằng BCrypt)");
                    System.out.println("========================================");
                } else {
                    System.err.println("ERROR: Không thể verify SEC_ADMIN sau khi insert!");
                }
            } else {
                System.out.println("SEC_ADMIN đã tồn tại trong APP_LOGIN_USER");
            }
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("ERROR khi khởi tạo SEC_ADMIN:");
            System.err.println("Message: " + e.getMessage());
            System.err.println("Có thể table APP_LOGIN_USER chưa được tạo!");
            System.err.println("Vui lòng chạy script: init_app_login_user.sql");
            System.err.println("========================================");
            e.printStackTrace();
        }
    }
}

