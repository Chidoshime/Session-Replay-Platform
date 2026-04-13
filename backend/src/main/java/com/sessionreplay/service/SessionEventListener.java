package com.sessionreplay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sessionreplay.event.SessionEventBatch;
import com.sessionreplay.model.Session;
import com.sessionreplay.model.SessionEvent;
import com.sessionreplay.repository.SessionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionEventListener {

    private final SessionService sessionService;
    private final SessionEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "session.events.queue")
    @Transactional
    public void handleSessionEvents(SessionEventBatch batch, 
                                    @Header(AmqpHeaders.CHANNEL) com.rabbitmq.client.Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("📥 Received batch for session: {}", batch.getSessionId());
            
            if (batch.getEvents() == null || batch.getEvents().isEmpty()) {
                log.warn("Empty event batch received");
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Создаем или обновляем сессию
            Session session = sessionService.getOrCreateSession(
                batch.getSessionId(),
                getMetadataValue(batch, "userAgent"),
                getMetadataValue(batch, "ip"),
                batch.getEvents().get(0).getUrl()
            );

            // Сохраняем события
            List<SessionEvent> savedEvents = saveEvents(batch);
            
            // Обновляем счетчик событий
            sessionService.updateEventCount(session.getSessionId(), savedEvents.size());

            channel.basicAck(deliveryTag, false);
            log.info("✅ Successfully processed {} events for session {}", savedEvents.size(), batch.getSessionId());
            
        } catch (Exception e) {
            log.error("❌ Error processing session events for sessionId: {}", batch.getSessionId(), e);
            try {
                // Отключаем requeue (третий параметр false), чтобы не зацикливать ошибочные сообщения
                channel.basicNack(deliveryTag, false, false); 
            } catch (Exception ex) {
                log.error("Error sending nack", ex);
            }
        }
    }

    private List<SessionEvent> saveEvents(SessionEventBatch batch) {
        List<SessionEvent> eventsToSave = new ArrayList<>();
        
        for (SessionEventBatch.EventDTO event : batch.getEvents()) {
            String dataJson = "{}";
            try {
                if (event.getData() != null) {
                    dataJson = objectMapper.writeValueAsString(event.getData());
                }
            } catch (Exception e) {
                log.error("Error serializing event data", e);
            }

            Integer eventTypeVal = event.getEventType();
            if (eventTypeVal == null) {
                log.warn("Event type is null! Event: {}", event);
                eventTypeVal = 0; 
            }

            Long timestampVal = event.getTimestamp(); 
            if (timestampVal == null) {
                timestampVal = 0L;
            }

            SessionEvent sessionEvent = SessionEvent.builder()
                .sessionId(batch.getSessionId())
                .timestamp(timestampVal)
                .eventType(eventTypeVal)
                .data(dataJson)
                .url(event.getUrl())
                .viewportWidth(event.getViewportWidth())
                .viewportHeight(event.getViewportHeight())
                .build();
            
            eventsToSave.add(sessionEvent);
        }

        List<SessionEvent> savedEvents = new ArrayList<>();
        eventRepository.saveAll(eventsToSave).forEach(savedEvents::add);
        
        return savedEvents;
    }

    // Исправленный метод с приведением типа
    private String getMetadataValue(SessionEventBatch batch, String key) {
        if (batch.getMetadata() != null) {
            Object value = batch.getMetadata().get(key);
            return value != null ? value.toString() : null;
        }
        return null;
    }
}