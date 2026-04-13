package com.sessionreplay.repository;

import com.sessionreplay.model.SessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {
    
    @Query("SELECT e FROM SessionEvent e WHERE e.sessionId = :sessionId ORDER BY e.timestamp ASC")
    List<SessionEvent> findBySessionIdOrderByTimestamp(String sessionId);
    
    @Query("SELECT COUNT(e) FROM SessionEvent e WHERE e.sessionId = :sessionId")
    long countBySessionId(String sessionId);
}
