package com.xinhao.chat.controller;

import com.xinhao.chat.dto.LoginRequest;
import com.xinhao.chat.dto.LoginResponse;
import com.xinhao.chat.dto.RegisterRequest;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.service.TokenService;
import com.xinhao.chat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            // Auto-login after register
            String token = tokenService.createToken(user);
            return ResponseEntity.ok(new LoginResponse(token, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request);
            String token = tokenService.createToken(user);
            return ResponseEntity.ok(new LoginResponse(token, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
