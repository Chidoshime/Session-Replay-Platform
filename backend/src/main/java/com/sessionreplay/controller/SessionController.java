package com.sessionreplay.controller;

import com.sessionreplay.event.SessionEventBatch;
import com.sessionreplay.model.SessionEvent;
import com.sessionreplay.service.SessionService;
import com.sessionreplay.repository.SessionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sessionreplay.config.RabbitMQConfig.SESSION_EVENTS_EXCHANGE;
import static com.sessionreplay.config.RabbitMQConfig.SESSION_EVENTS_ROUTING_KEY;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${session.replay.allowed-origin:http://localhost:3000}")
public class SessionController {

    private final RabbitTemplate rabbitTemplate;
    private final SessionService sessionService;
    private final SessionEventRepository eventRepository;

    @Value("${session.replay.buffer-size:100}")
    private int bufferSize;

    @Value("${session.replay.api-key:}")
    private String apiKey;

    /**
     * Принимает пакет событий сессии от клиента
     */
    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> ingestEvents(
            @RequestBody SessionEventBatch batch,
            @RequestHeader(value = "X-API-Key", required = false) String requestApiKey,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        if (!isApiKeyValid(requestApiKey)) {
            return unauthorized();
        }
        
        log.debug("Received {} events for session {}", 
            batch.getEvents() != null ? batch.getEvents().size() : 0, 
            batch.getSessionId());

        // Добавляем метаданные из заголовков
        if (batch.getMetadata() == null) {
            batch.setMetadata(new HashMap<>());
        }
        if (userAgent != null) {
            batch.getMetadata().put("userAgent", userAgent);
        }
        if (forwardedFor != null) {
            batch.getMetadata().put("ip", forwardedFor.split(",")[0].trim());
        }

        // Отправляем в RabbitMQ для асинхронной обработки
        rabbitTemplate.convertAndSend(SESSION_EVENTS_EXCHANGE, SESSION_EVENTS_ROUTING_KEY, batch);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", batch.getSessionId());
        response.put("eventsReceived", batch.getEvents() != null ? batch.getEvents().size() : 0);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Получение сессии по ID
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-API-Key", required = false) String requestApiKey) {
        if (!isApiKeyValid(requestApiKey)) {
            return unauthorized();
        }
        log.info("Fetching session: {}", sessionId);
        
        return sessionService.getSession(sessionId)
            .map(session -> {
                Map<String, Object> response = new HashMap<>();
                response.put("id", session.getId());
                response.put("sessionId", session.getSessionId());
                response.put("startTime", session.getStartTime());
                response.put("endTime", session.getEndTime());
                response.put("url", session.getUrl());
                response.put("userAgent", session.getUserAgent());
                response.put("eventCount", session.getEventCount());
                response.put("active", session.getActive());
                response.put("metadata", session.getMetadata());
                
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получение всех событий сессии для воспроизведения
     */
    @GetMapping("/{sessionId}/events")
    public ResponseEntity<?> getSessionEvents(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-API-Key", required = false) String requestApiKey) {
        if (!isApiKeyValid(requestApiKey)) {
            return unauthorized();
        }
        log.info("Fetching events for session: {}", sessionId);
        
        List<SessionEvent> events = eventRepository.findBySessionIdOrderByTimestamp(sessionId);
        return ResponseEntity.ok(events);
    }

    /**
     * Завершение сессии (может вызываться клиентом при уходе со страницы)
     */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-API-Key", required = false) String requestApiKey) {
        if (!isApiKeyValid(requestApiKey)) {
            return unauthorized();
        }
        log.info("Ending session: {}", sessionId);
        
        sessionService.endSession(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", sessionId);
        
        return ResponseEntity.ok(response);
    }

    private boolean isApiKeyValid(String requestApiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return true;
        }
        return apiKey.equals(requestApiKey);
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
