package ru.practicum.stats.server.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.server.model.StatsEntity;

@Component
public class StatsMapper {

    public StatsEntity toEntity(EndpointHit hit) {
        return StatsEntity.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .build();
    }

    public EndpointHit toDto(StatsEntity entity) {
        return EndpointHit.builder()
                .id(entity.getId())
                .app(entity.getApp())
                .uri(entity.getUri())
                .ip(entity.getIp())
                .timestamp(entity.getTimestamp())
                .build();
    }
}