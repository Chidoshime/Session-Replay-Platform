package com.sessionreplay.service;

import com.sessionreplay.event.SessionEventBatch;
import com.sessionreplay.model.Session;
import com.sessionreplay.model.SessionEvent;
import com.sessionreplay.repository.SessionEventRepository;
import com.sessionreplay.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionEventListener {

    private final SessionService sessionService;
    private final SessionEventRepository eventRepository;

    @RabbitListener(queues = "session.events.queue")
    @Transactional
    public void handleSessionEvents(SessionEventBatch batch, 
                                    @Header(AmqpHeaders.CHANNEL) com.rabbitmq.client.Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.debug("Processing session events for sessionId: {}", batch.getSessionId());
            
            // Создаем или обновляем сессию
            Session session = sessionService.getOrCreateSession(
                batch.getSessionId(),
                batch.getMetadata() != null ? batch.getMetadata().get("userAgent") : null,
                batch.getMetadata() != null ? batch.getMetadata().get("ip") : null,
                batch.getEvents().isEmpty() ? null : batch.getEvents().get(0).getUrl()
            );

            // Сохраняем события
            List<SessionEvent> savedEvents = saveEvents(batch);
            
            // Обновляем счетчик событий
            sessionService.updateEventCount(session.getSessionId(), savedEvents.size());

            channel.basicAck(deliveryTag, false);
            log.info("Successfully processed {} events for session {}", savedEvents.size(), batch.getSessionId());
            
        } catch (Exception e) {
            log.error("Error processing session events for sessionId: {}", batch.getSessionId(), e);
            try {
                channel.basicNack(deliveryTag, false, true); // Requeue
            } catch (Exception ex) {
                log.error("Error sending nack", ex);
            }
        }
    }

    private List<SessionEvent> saveEvents(SessionEventBatch batch) {
        List<SessionEvent> events = batch.getEvents().stream()
            .map(event -> SessionEvent.builder()
                .sessionId(batch.getSessionId())
                .timestamp(event.getTimestamp())
                .eventType(event.getType())
                .data(event.getData())
                .url(event.getUrl())
                .viewportWidth(event.getViewportWidth())
                .viewportHeight(event.getViewportHeight())
                .build())
            .toList();
        
        return eventRepository.saveAll(events);
    }
}
