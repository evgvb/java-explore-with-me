package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final StatsMapper statsMapper;

    @Override
    @Transactional
    public EndpointHit saveHit(EndpointHit hit) {
        log.debug("Сохранение информации о запросе: app={}, uri={}, ip={}",
                hit.getApp(), hit.getUri(), hit.getIp());

        var entity = statsMapper.toEntity(hit);
        var saved = statsRepository.save(entity);

        log.debug("Запрос сохранен с ID: {}", saved.getId());
        return statsMapper.toDto(saved);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.debug("Получение статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        // Валидация дат: дата начала не должна быть позже даты конца
        if (start == null || end == null) {
            throw new IllegalArgumentException("Даты начала и окончания не могут быть null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Object[]> results;

        if (unique) {
            results = statsRepository.findUniqueHits(start, end, uris);
        } else {
            results = statsRepository.findAllHits(start, end, uris);
        }

        List<ViewStats> stats = results.stream()
                .map(row -> ViewStats.builder()
                        .uri((String) row[0])
                        .app((String) row[1])
                        .hits((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        log.debug("Найдено {} записей статистики", stats.size());
        return stats;
    }
}