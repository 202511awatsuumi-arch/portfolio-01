package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/admin/chat")
@PreAuthorize("isAuthenticated()")
public class AdminChatController {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> body) {

        String userMessage = body.get("message");

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> part = Map.of("text", userMessage);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(content),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 500
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request =
            new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                GEMINI_URL + apiKey, request, Map.class);

            List candidates = (List) response.getBody().get("candidates");
            Map candidate = (Map) candidates.get(0);
            Map contentMap = (Map) candidate.get("content");
            List parts = (List) contentMap.get("parts");
            Map firstPart = (Map) parts.get(0);
            String text = (String) firstPart.get("text");

            return ResponseEntity.ok(Map.of("reply", text));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("reply", "エラーが発生しました: " + e.getMessage()));
        }
    }
}
