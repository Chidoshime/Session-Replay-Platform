package com.sessionreplay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import javax.persistence.*;

@Entity
@Table(name = "session_events", indexes = {
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_timestamp", columnList = "ts_ms")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "ts_ms", nullable = false)
    private Long timestamp;

    @Column(name = "event_type", nullable = false)
    private Integer eventType;

    // Храним JSON события как строку (TEXT/JSONB)
    @Column(name = "data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String data;

    @Column(name = "url")
    private String url;

    @Column(name = "viewport_width")
    private Integer viewportWidth;

    @Column(name = "viewport_height")
    private Integer viewportHeight;
}
