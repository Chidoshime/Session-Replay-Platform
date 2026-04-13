package com.sessionreplay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sessionreplay.model.Session;
import com.sessionreplay.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    @Value("${session.replay.max-session-duration-ms:3600000}")
    private long maxSessionDurationMs;

    @Transactional
    public Session getOrCreateSession(String sessionId, String userAgent, String ip, String url) {
        Optional<Session> existingSession = sessionRepository.findActiveBySessionId(sessionId);
        
        if (existingSession.isPresent()) {
            Session session = existingSession.get();
            
            // Проверяем, не истекло ли время сессии
            long duration = Instant.now().toEpochMilli() - session.getStartTime().toEpochMilli();
            if (duration > maxSessionDurationMs) {
                log.info("Session {} expired, creating new one", sessionId);
                sessionRepository.deactivateSession(sessionId, Instant.now());
                return createNewSession(sessionId, userAgent, ip, url);
            }
            
            // Обновляем информацию о сессии если нужно
            if (session.getUserAgent() == null && userAgent != null) {
                session.setUserAgent(userAgent);
            }
            if (session.getIp() == null && ip != null) {
                session.setIp(ip);
            }
            if (session.getUrl() == null && url != null) {
                session.setUrl(url);
            }
            
            return sessionRepository.save(session);
        } else {
            return createNewSession(sessionId, userAgent, ip, url);
        }
    }

    private Session createNewSession(String sessionId, String userAgent, String ip, String url) {
        log.info("Creating new session: {}", sessionId);
        
        Map<String, Object> metadata = new HashMap<>();
        if (userAgent != null) {
            metadata.put("userAgent", userAgent);
        }
        if (ip != null) {
            metadata.put("ip", ip);
        }
        
        Session session = Session.builder()
            .sessionId(sessionId)
            .startTime(Instant.now())
            .userAgent(userAgent)
            .ip(ip)
            .url(url)
            .metadata(toJson(metadata))
            .active(true)
            .eventCount(0)
            .build();
        
        return sessionRepository.save(session);
    }

    @Transactional
    public void updateEventCount(String sessionId, int eventCount) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setEventCount(session.getEventCount() + eventCount);
            sessionRepository.save(session);
        });
    }

    @Transactional
    public void endSession(String sessionId) {
        sessionRepository.deactivateSession(sessionId, Instant.now());
        log.info("Session {} ended", sessionId);
    }

    public Optional<Session> getSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize session metadata to JSON", e);
            return "{}";
        }
    }
}
