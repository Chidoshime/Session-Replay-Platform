package com.sessionreplay.repository;

import com.sessionreplay.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionId(String sessionId);

    // Метод для получения всех сессий, отсортированных по времени начала
    List<Session> findAllByOrderByStartTimeDesc();

    // Метод для поиска активной сессии (если нужно)
    default Optional<Session> findActiveBySessionId(String sessionId) {
        return findBySessionId(sessionId);
    }

    // Метод для деактивации сессии
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.active = false, s.endTime = :endTime WHERE s.sessionId = :sessionId")
    void deactivateSession(@Param("sessionId") String sessionId, @Param("endTime") Instant endTime);
}