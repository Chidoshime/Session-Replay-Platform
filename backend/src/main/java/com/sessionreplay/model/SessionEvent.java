package com.sessionreplay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "session_events", indexes = {
    @Index(name = "idx_session_id", columnList = "sessionId"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
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

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "type", nullable = false)
    private String type;

    // Храним JSON события как строку (TEXT/JSONB)
    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    @Column(name = "index", nullable = false)
    private Integer index;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }
}
