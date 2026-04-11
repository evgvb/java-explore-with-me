import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.practicum.stats.dto.ViewStats;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты DTO ViewStats")
class ViewStatsTest {

    private static final Logger log = LoggerFactory.getLogger(ViewStatsTest.class);
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void testBuilder() {
        log.info("Тест создания ViewStats");

        ViewStats stats = ViewStats.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .hits(42L)
                .build();

        assertThat(stats).isNotNull();
        assertThat(stats.getApp()).isEqualTo("ewm-main-service");
        assertThat(stats.getUri()).isEqualTo("/events/1");
        assertThat(stats.getHits()).isEqualTo(42L);

        log.info("Создан ViewStats: app={}, uri={}, hits={}",
                stats.getApp(), stats.getUri(), stats.getHits());
    }

    @Test
    void testSerialization() throws Exception {
        log.info("ViewStats в JSON");

        ViewStats stats = ViewStats.builder()
                .app("test-service")
                .uri("/api/test")
                .hits(100L)
                .build();

        log.debug("Исходный объект: app={}, uri={}, hits={}",
                stats.getApp(), stats.getUri(), stats.getHits());

        String json = objectMapper.writeValueAsString(stats);

        assertThat(json).isNotNull();
        assertThat(json).contains("test-service");
        assertThat(json).contains("/api/test");
        assertThat(json).contains("100");

        log.info("JSON результат: {}", json);

    }

    @Test
    void testDeserialization() throws Exception {
        log.info("ViewStats из JSON");

        String json = "{\"app\":\"ewm-main-service\",\"uri\":\"/events/500\",\"hits\":999}";

        log.debug("JSON: {}", json);

        ViewStats stats = objectMapper.readValue(json, ViewStats.class);

        assertThat(stats).isNotNull();
        assertThat(stats.getApp()).isEqualTo("ewm-main-service");
        assertThat(stats.getUri()).isEqualTo("/events/500");
        assertThat(stats.getHits()).isEqualTo(999L);

        log.info("Получен объект: app={}, uri={}, hits={}",
                stats.getApp(), stats.getUri(), stats.getHits());
    }

    @Test
    void testEqualsAndHashCode() {
        log.info("Тест сравнения ViewStats");

        ViewStats stats1 = ViewStats.builder()
                .app("service1")
                .uri("/path1")
                .hits(10L)
                .build();

        ViewStats stats2 = ViewStats.builder()
                .app("service1")
                .uri("/path1")
                .hits(10L)
                .build();

        ViewStats stats3 = ViewStats.builder()
                .app("service2")
                .uri("/path2")
                .hits(20L)
                .build();

        assertThat(stats1).isEqualTo(stats2);
        assertThat(stats1).isNotEqualTo(stats3);
        assertThat(stats1.hashCode()).isEqualTo(stats2.hashCode());

        log.info("stats1 == stats2: {}", stats1.equals(stats2));
        log.info("stats1 == stats3: {}", stats1.equals(stats3));
    }
}