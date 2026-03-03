package com.xinhao.chat.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String nickname;
    private String signature;
    private Integer gender;
    private String avatar;
}
