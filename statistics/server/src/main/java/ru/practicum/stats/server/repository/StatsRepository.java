package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.stats.server.model.StatsEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<StatsEntity, Long> {

    @Query("SELECT s.uri, s.app, COUNT(s.id) as hits " +
            "FROM StatsEntity s " +
            "WHERE s.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR s.uri IN :uris) " +
            "GROUP BY s.uri, s.app " +
            "ORDER BY hits DESC")
    List<Object[]> findAllHits(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end,
                               @Param("uris") List<String> uris);

    @Query("SELECT s.uri, s.app, COUNT(DISTINCT s.ip) as hits " +
            "FROM StatsEntity s " +
            "WHERE s.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR s.uri IN :uris) " +
            "GROUP BY s.uri, s.app " +
            "ORDER BY hits DESC")
    List<Object[]> findUniqueHits(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("uris") List<String> uris);
}