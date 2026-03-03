package com.xinhao.chat.dto;

import lombok.Data;
import java.util.List;

@Data
public class MomentRequest {
    private String content;
    private List<String> images;
}
