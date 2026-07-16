package com.teamcollab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class AIService {

    @Value("${ark.api.key}")
    private String apiKey;

    private final String API_URL = "https://api.deepseek.com/chat/completions";
    private final String MODEL_NAME = "deepseek-chat";
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public AIService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String callAI(String systemPrompt, String userPrompt) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalStateException("AI service is disabled: set DEEPSEEK_API_KEY to enable it");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL_NAME);

            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
            }

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = mapper.readTree(response.getBody());
                return root.path("choices").get(0).path("message").path("content").asText();
            } else {
                throw new RuntimeException("AI API call failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke AI service: " + e.getMessage(), e);
        }
    }
}
