package com.example.demo.config;

import com.example.demo.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                           Object handler) throws Exception {
        HttpSession session = request.getSession();
        
        // Cho phép truy cập login page và static resources
        String path = request.getRequestURI();
        if (path.startsWith("/login") || 
            path.startsWith("/css/") || 
            path.startsWith("/js/") || 
            path.startsWith("/images/") ||
            path.equals("/")) {
            return true;
        }
        
        // Kiểm tra đã đăng nhập chưa
        if (!authenticationService.isLoggedIn(session)) {
            response.sendRedirect("/login");
            return false;
        }
        
        return true;
    }
}

