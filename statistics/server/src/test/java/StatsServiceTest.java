import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.model.StatsEntity;
import ru.practicum.stats.server.repository.StatsRepository;
import ru.practicum.stats.server.service.StatsServiceImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты StatsService")
class StatsServiceTest {

    private static final Logger log = LoggerFactory.getLogger(StatsServiceTest.class);

    @Mock
    private StatsRepository statsRepository;

    @Mock
    private StatsMapper statsMapper;

    @InjectMocks
    private StatsServiceImpl statsService;

    @Test
    void testSaveHit() {
        log.info("Тест сохранения hit");

        LocalDateTime timestamp = LocalDateTime.now();

        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("127.0.0.1")
                .timestamp(timestamp)
                .build();

        StatsEntity entity = StatsEntity.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("127.0.0.1")
                .timestamp(timestamp)
                .build();

        StatsEntity savedEntity = StatsEntity.builder()
                .id(1L)
                .app("test-app")
                .uri("/test/1")
                .ip("127.0.0.1")
                .timestamp(timestamp)
                .build();

        EndpointHit savedHit = EndpointHit.builder()
                .id(1L)
                .app("test-app")
                .uri("/test/1")
                .ip("127.0.0.1")
                .timestamp(timestamp)
                .build();

        log.debug("Входной hit: app={}, uri={}", hit.getApp(), hit.getUri());

        when(statsMapper.toEntity(hit)).thenReturn(entity);
        when(statsRepository.save(entity)).thenReturn(savedEntity);
        when(statsMapper.toDto(savedEntity)).thenReturn(savedHit);

        EndpointHit result = statsService.saveHit(hit);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        log.info("Результат: сохранен hit с id={}", result.getId());

    }

    @Test
    void testGetStatsAll() {
        log.info("Тест получения всех записей");

        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 16, 0, 0, 0);

        List<Object[]> mockResults = Arrays.asList(
                new Object[]{"/events/1", "app1", 10L},
                new Object[]{"/events/2", "app1", 5L},
                new Object[]{"/users/1", "app2", 3L}
        );

        log.debug("Параметры: start={}, end={}, unique=false", start, end);
        log.debug("Mock-данные: {} записей", mockResults.size());

        when(statsRepository.findAllHits(eq(start), eq(end), isNull()))
                .thenReturn(mockResults);

        List<ViewStats> result = statsService.getStats(start, end, null, false);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getHits()).isEqualTo(10L);

        log.info("Результат: получено {} записей статистики", result.size());
    }

    @Test
    void testGetStatsUnique() {
        log.info("Тест получения записи");

        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 16, 0, 0, 0);

        List<Object[]> mockResults = Arrays.asList(
                new Object[]{"/events/1", "app1", 5L},
                new Object[]{"/events/2", "app1", 3L}
        );

        when(statsRepository.findUniqueHits(eq(start), eq(end), isNull()))
                .thenReturn(mockResults);

        List<ViewStats> result = statsService.getStats(start, end, null, true);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getHits()).isEqualTo(5L);

        log.info("Результат (уникальные): получено {} записей", result.size());
    }

    @Test
    void testGetStatsEmpty() {
        log.info("Тест получения пустого");

        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 16, 0, 0, 0);

        when(statsRepository.findAllHits(eq(start), eq(end), isNull()))
                .thenReturn(List.of());

        List<ViewStats> result = statsService.getStats(start, end, null, false);

        assertThat(result).isEmpty();

        log.info("Результат нет данных за период");
    }
}