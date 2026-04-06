package com.sessionreplay.event;

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
    
    private Map<String, String> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventDTO {
        private Integer timestamp;
        private String type;
        private Map<String, Object> data;
        private String url;
        private Integer viewportWidth;
        private Integer viewportHeight;
    }
}
