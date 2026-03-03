package com.xinhao.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhao.chat.entity.Message;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.repository.MessageRepository;
import com.xinhao.chat.repository.UserRepository;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AiService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final String API_KEY = "ms-e2dcd9b4-86eb-4c7e-978c-800c329be4a8";
    private static final String API_URL = "https://api-inference.modelscope.cn/v1/chat/completions";
    private static final String MODEL = "ZhipuAI/GLM-5"; 
    
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public AiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    private User getOrCreateAiUser() {
        return userRepository.findByUsername("ai_assistant")
                .map(user -> {
                    // Update avatar if needed
                    if (!"/uploads/ai_avatar.jpg".equals(user.getAvatar())) {
                        user.setAvatar("/uploads/ai_avatar.jpg");
                        return userRepository.save(user);
                    }
                    return user;
                })
                .orElseGet(() -> {
                    User aiUser = new User();
                    aiUser.setUsername("ai_assistant");
                    aiUser.setNickname("炘灏科技");
                    aiUser.setPassword(passwordEncoder.encode("123456"));
                    aiUser.setAvatar("/uploads/ai_avatar.jpg");
                    aiUser.setSignature("我是您的AI助手");
                    return userRepository.save(aiUser);
                });
    }

    public void streamChat(Long userId, String userMessage) {
        // 1. Find user
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        
        // 2. Create a placeholder message for AI response
        User aiUser = getOrCreateAiUser();

        Message aiMessage = new Message();
        aiMessage.setSender(aiUser);
        aiMessage.setReceiver(user);
        aiMessage.setContent(""); // Start empty
        aiMessage.setType("text");
        aiMessage.setCreateTime(LocalDateTime.now());
        
        Message savedAiMessage = messageRepository.save(aiMessage);
        Long messageId = savedAiMessage.getId();
        
        // Notify frontend that AI started replying (empty message)
        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/messages", savedAiMessage);

        // 3. Prepare request to ModelScope
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", MODEL);
        payload.put("stream", true);
        payload.put("messages", Collections.singletonList(
                Map.of("role", "user", "content", userMessage)
        ));

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        // 4. Start SSE stream
        EventSource.Factory factory = EventSources.createFactory(client);
        factory.newEventSource(request, new EventSourceListener() {
            private final StringBuilder fullContent = new StringBuilder();

            @Override
            public void onOpen(EventSource eventSource, Response response) {
                // Connection opened
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                if ("[DONE]".equals(data)) {
                    return;
                }

                try {
                    JsonNode node = objectMapper.readTree(data);
                    if (node.has("choices") && node.get("choices").isArray() && !node.get("choices").isEmpty()) {
                        JsonNode choice = node.get("choices").get(0);
                        if (choice.has("delta")) {
                            JsonNode delta = choice.get("delta");
                            String content = "";
                            
                            // Check for reasoning_content (ModelScope/DeepSeek R1 style)
                            if (delta.has("reasoning_content") && !delta.get("reasoning_content").isNull()) {
                                String reasoning = delta.get("reasoning_content").asText();
                                if (reasoning != null && !reasoning.isEmpty()) {
                                    // Just append reasoning content for now, or format it
                                    content += reasoning;
                                }
                            }
                            
                            if (delta.has("content") && !delta.get("content").isNull()) {
                                String ans = delta.get("content").asText();
                                if (ans != null && !ans.isEmpty()) {
                                    content += ans;
                                }
                            }
                            
                            if (!content.isEmpty()) {
                                fullContent.append(content);
                                
                                // Send chunk to frontend via WebSocket
                                Map<String, Object> update = new HashMap<>();
                                update.put("type", "stream"); // Custom type for stream updates
                                update.put("id", messageId); // Use 'id' to match message
                                update.put("content", content); // Send delta
                                // Also send senderId to filter on frontend
                                update.put("senderId", aiUser.getId());
                                
                                messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/messages", update);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                // Update DB with final content
                // Re-fetch message to avoid detached entity issues if session closed
                // But simple save works if ID is set.
                savedAiMessage.setContent(fullContent.toString());
                messageRepository.save(savedAiMessage);
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                // Only log error if it's not a normal cancellation
                 if (fullContent.length() > 0) {
                    savedAiMessage.setContent(fullContent.toString());
                    messageRepository.save(savedAiMessage);
                } else {
                     // If completely failed
                     String errorMessage = "AI 服务暂时不可用，请稍后再试。";
                     if (response != null) {
                         try {
                             String body = response.body() != null ? response.body().string() : "No body";
                             System.err.println("AI Service Error: " + response.code() + " " + body);
                             // errorMessage += " (" + response.code() + ")"; // Optional: show error code
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     } else {
                         System.err.println("AI Service Error: " + t.getMessage());
                         t.printStackTrace();
                     }
                     
                     savedAiMessage.setContent(errorMessage);
                     messageRepository.save(savedAiMessage);
                     
                     Map<String, Object> update = new HashMap<>();
                     update.put("type", "stream");
                     update.put("id", messageId);
                     update.put("content", errorMessage);
                     messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/messages", update);
                }
            }
        });
    }
}
