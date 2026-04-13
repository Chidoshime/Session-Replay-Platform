package com.sessionreplay.repository;

import com.sessionreplay.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    Optional<Session> findBySessionId(String sessionId);
    
    @Query("SELECT s FROM Session s WHERE s.sessionId = :sessionId AND s.active = true")
    Optional<Session> findActiveBySessionId(String sessionId);
    
    @Modifying
    @Query("UPDATE Session s SET s.active = false, s.endTime = :endTime WHERE s.sessionId = :sessionId")
    int deactivateSession(String sessionId, Instant endTime);
}
