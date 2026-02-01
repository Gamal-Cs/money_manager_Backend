package com.moneymanager.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/debug")
@Slf4j
public class DebugController {

    @GetMapping("/paths")
    public ResponseEntity<Map<String, String>> getPaths() {
        return ResponseEntity.ok(Map.of(
                "baseUrl", "http://localhost:8080",
                "contextPath", "/api/v1.0",
                "authLogin", "http://localhost:8080/api/v1.0/auth/login",
                "authRegister", "http://localhost:8080/api/v1.0/auth/register",
                "healthCheck", "http://localhost:8080/api/v1.0/test/health"
        ));
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echoRequest(
            @RequestBody Map<String, Object> body,
            @RequestHeader Map<String, String> headers) {

        log.info("Headers received: {}", headers);
        log.info("Body received: {}", body);

        return ResponseEntity.ok(Map.of(
                "message", "Request received successfully",
                "body", body,
                "headers", headers.keySet(),
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}