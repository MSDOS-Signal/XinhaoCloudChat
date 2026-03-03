package com.xinhao.chat.dto;

import com.xinhao.chat.entity.User;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private User user;
}
