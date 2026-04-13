package com.sessionreplay.controller;

import com.sessionreplay.event.SessionEventBatch;
import com.sessionreplay.model.Session;
import com.sessionreplay.model.SessionEvent;
import com.sessionreplay.repository.SessionEventRepository;
import com.sessionreplay.repository.SessionRepository;
import com.sessionreplay.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;
    private final SessionEventRepository eventRepository;
    private final SessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> ingestEvents(
            @RequestBody SessionEventBatch batch,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        
        if (batch.getEvents() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No events provided"));
        }

        log.info("📥 Received {} events for session {}", batch.getEvents().size(), batch.getSessionId());

        // Обработка метаданных
        if (batch.getMetadata() == null) {
            batch.setMetadata(new HashMap<>());
        }
        if (userAgent != null) {
            batch.getMetadata().put("userAgent", userAgent);
        }
        if (forwardedFor != null) {
            batch.getMetadata().put("ip", forwardedFor.split(",")[0].trim());
        }

        try {
            saveEventsSync(batch);
            log.info("✅ Events saved directly to DB");
        } catch (Exception e) {
            log.error("❌ Critical error saving events", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", batch.getSessionId());
        response.put("eventsReceived", batch.getEvents().size());
        
        return ResponseEntity.ok(response);
    }

        /**
     * Метод для синхронного сохранения событий в БД
     */
    private void saveEventsSync(SessionEventBatch batch) {
        if (batch.getEvents() == null || batch.getEvents().isEmpty()) {
            return;
        }

        // 1. Безопасное получение метаданных
        String userAgent = null;
        String ip = null;
        
        if (batch.getMetadata() != null) {
            Object uaObj = batch.getMetadata().get("userAgent");
            if (uaObj != null) userAgent = uaObj.toString();
            
            Object ipObj = batch.getMetadata().get("ip");
            if (ipObj != null) ip = ipObj.toString();
        }

        // 2. Получение первого URL
        String firstUrl = null;
        for (SessionEventBatch.EventDTO e : batch.getEvents()) {
            if (e.getUrl() != null) {
                firstUrl = e.getUrl();
                break;
            }
        }

        // 3. Создаем или обновляем сессию
        Session session = sessionService.getOrCreateSession(
                batch.getSessionId(),
                userAgent,
                ip,
                firstUrl
        );

        // 4. Конвертируем и сохраняем события
        List<SessionEvent> eventsToSave = new java.util.ArrayList<>();
        
        for (SessionEventBatch.EventDTO dto : batch.getEvents()) {
            String dataJson = "{}";
            try {
                if (dto.getData() != null) {
                    dataJson = objectMapper.writeValueAsString(dto.getData());
                }
            } catch (Exception e) {
                log.error("Error serializing event data", e);
            }

            Integer eventType = dto.getEventType();
            if (eventType == null) eventType = 0;

            // Исправление типа timestamp: rrweb присылает Long, модель ждет Long
            Long timestamp = dto.getTimestamp();
            if (timestamp == null) {
                timestamp = System.currentTimeMillis();
            }

            SessionEvent event = SessionEvent.builder()
                    .sessionId(batch.getSessionId())
                    .timestamp(timestamp) 
                    .eventType(eventType)
                    .data(dataJson)
                    .url(dto.getUrl())
                    .viewportWidth(dto.getViewportWidth())
                    .viewportHeight(dto.getViewportHeight())
                    .build();
            
            eventsToSave.add(event);
        }

        eventRepository.saveAll(eventsToSave);
        
        // Обновляем счетчик
        sessionService.updateEventCount(batch.getSessionId(), eventsToSave.size());
        
        log.info("💾 Saved {} events to DB.", eventsToSave.size());
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findBySessionId(sessionId);
        return sessionOpt.map(session -> {
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
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{sessionId}/events")
    public ResponseEntity<List<SessionEvent>> getSessionEvents(@PathVariable String sessionId) {
        List<SessionEvent> events = eventRepository.findBySessionIdOrderByTimestamp(sessionId);
        return ResponseEntity.ok(events);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSessions() {
        List<Session> sessions = sessionRepository.findAllByOrderByStartTimeDesc();
        List<Map<String, Object>> response = sessions.stream().map(session -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", session.getSessionId());
            map.put("startTime", session.getStartTime());
            map.put("url", session.getUrl());
            map.put("eventCount", session.getEventCount());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endSession(@PathVariable String sessionId) {
        sessionService.endSession(sessionId);
        return ResponseEntity.ok(Map.of("success", true, "sessionId", sessionId));
    }
}