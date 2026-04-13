package com.sessionreplay.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEventBatch {
    
    private String sessionId;
    private String userId;
    private List<EventDTO> events;
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventDTO {
        private Long timestamp;
        
        // ВАЖНО: Эта аннотация связывает JSON-поле "type" с Java-полем "eventType"
        @JsonProperty("type") 
        private Integer eventType;
        
        private Map<String, Object> data;
        private String url;
        private Integer viewportWidth;
        private Integer viewportHeight;
    }
}