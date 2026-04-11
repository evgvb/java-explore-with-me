import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты StatsClient")
class StatsClientTest {

    private static final Logger log = LoggerFactory.getLogger(StatsClientTest.class);

    @Test
    void ClientCreationTest() {
        log.info("Тест создания StatsClient");

        StatsClient client = new StatsClient("http://localhost:9090", new RestTemplateBuilder());

        assertThat(client).isNotNull();

        log.info("StatsClient успешно создан");
    }

    @Test
    void CreateEndpointHitTest() {
        log.info("Тест создания EndpointHit");

        LocalDateTime now = LocalDateTime.now();

        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(now)
                .build();

        assertThat(hit).isNotNull();
        assertThat(hit.getApp()).isEqualTo("test-app");
        assertThat(hit.getUri()).isEqualTo("/test");
        assertThat(hit.getIp()).isEqualTo("127.0.0.1");
        assertThat(hit.getTimestamp()).isEqualTo(now);

        log.info("EndpointHit создан: app={}, uri={}", hit.getApp(), hit.getUri());
    }

    @Test
    void CreateViewStatsTest() {
        log.info("Тест создания ViewStats");

        ViewStats stats = ViewStats.builder()
                .app("test-app")
                .uri("/test")
                .hits(100L)
                .build();

        assertThat(stats).isNotNull();
        assertThat(stats.getApp()).isEqualTo("test-app");
        assertThat(stats.getUri()).isEqualTo("/test");
        assertThat(stats.getHits()).isEqualTo(100L);

        log.info("ViewStats создан: app={}, hits={}", stats.getApp(), stats.getHits());
    }

    @Test
    void EndpointHitSerializationTest() {
        log.info("Тест сериализации/десериализации EndpointHit");

        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 12, 0, 0);

        EndpointHit original = EndpointHit.builder()
                .id(1L)
                .app("test-app")
                .uri("/test/uri")
                .ip("127.0.0.1")
                .timestamp(timestamp)
                .build();

        assertThat(original.getId()).isEqualTo(1L);
        assertThat(original.getApp()).isEqualTo("test-app");
        assertThat(original.getUri()).isEqualTo("/test/uri");
        assertThat(original.getIp()).isEqualTo("127.0.0.1");
        assertThat(original.getTimestamp()).isEqualTo(timestamp);

        EndpointHit copy = new EndpointHit();
        copy.setId(original.getId());
        copy.setApp(original.getApp());
        copy.setUri(original.getUri());
        copy.setIp(original.getIp());
        copy.setTimestamp(original.getTimestamp());

        assertThat(copy).isEqualTo(original);

        log.info("Сериализация/десериализация работает корректно");
    }
}