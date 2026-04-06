package com.sessionreplay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private String sessionId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "url")
    private String url;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ip;

    @Column(name = "active", columnDefinition = "boolean default true")
    private Boolean active = true;

    @Column(name = "event_count", columnDefinition = "integer default 0")
    private Integer eventCount = 0;

    // Храним JSON как строку для совместимости с Java 11 / Hibernate 5
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    private Long version;
}
