import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.practicum.stats.dto.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты DTO EndpointHit")
class EndpointHitTest {

    private static final Logger log = LoggerFactory.getLogger(EndpointHitTest.class);
    private ObjectMapper objectMapper;
    private DateTimeFormatter formatter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    @Test
    void testBuilder() {
        log.info("Создание EndpointHit");

        LocalDateTime timestamp = LocalDateTime.now();

        EndpointHit hit = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.100")
                .timestamp(timestamp)
                .build();

        assertThat(hit).isNotNull();
        assertThat(hit.getId()).isEqualTo(1L);
        assertThat(hit.getApp()).isEqualTo("ewm-main-service");
        assertThat(hit.getUri()).isEqualTo("/events/1");
        assertThat(hit.getIp()).isEqualTo("192.168.1.100");
        assertThat(hit.getTimestamp()).isEqualTo(timestamp);

        log.info("Создан EndpointHit: id={}, app={}, uri={}",
                hit.getId(), hit.getApp(), hit.getUri());
    }

    @Test
    void testSerialization() throws Exception {
        log.info("EndpointHit в JSON");

        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        EndpointHit hit = EndpointHit.builder()
                .id(10L)
                .app("test-service")
                .uri("/test/42")
                .ip("10.0.0.1")
                .timestamp(timestamp)
                .build();

        String json = objectMapper.writeValueAsString(hit);

        assertThat(json).isNotNull();
        assertThat(json).contains("test-service");
        assertThat(json).contains("/test/42");
        assertThat(json).contains("10.0.0.1");

        log.info("JSON результат: {}", json);
    }

    @Test
    void testDeserialization() throws Exception {
        log.info("EndpointHit из JSON");

        String json = "{\"id\":5,\"app\":\"ewm-main-service\",\"uri\":\"/events/100\",\"ip\":\"192.168.1.50\",\"timestamp\":\"2024-01-15 15:30:00\"}";

        EndpointHit hit = objectMapper.readValue(json, EndpointHit.class);

        assertThat(hit).isNotNull();
        assertThat(hit.getId()).isEqualTo(5L);
        assertThat(hit.getApp()).isEqualTo("ewm-main-service");
        assertThat(hit.getUri()).isEqualTo("/events/100");
        assertThat(hit.getIp()).isEqualTo("192.168.1.50");

        log.info("Получен объект: id={}, app={}", hit.getId(), hit.getApp());

    }

    @Test
    void testEquals() {
        log.info("Сравнение объектов");

        LocalDateTime timestamp = LocalDateTime.now();

        EndpointHit hit1 = EndpointHit.builder()
                .id(1L)
                .app("app1")
                .uri("/uri1")
                .ip("10.0.0.1")
                .timestamp(timestamp)
                .build();

        EndpointHit hit2 = EndpointHit.builder()
                .id(1L)
                .app("app1")
                .uri("/uri1")
                .ip("10.0.0.1")
                .timestamp(timestamp)
                .build();

        EndpointHit hit3 = EndpointHit.builder()
                .id(2L)
                .app("app2")
                .uri("/uri2")
                .ip("10.0.0.2")
                .timestamp(timestamp)
                .build();

        assertThat(hit1).isEqualTo(hit2);
        assertThat(hit1).isNotEqualTo(hit3);
        assertThat(hit1.hashCode()).isEqualTo(hit2.hashCode());

        log.info("hit1 == hit2: {}", hit1.equals(hit2));
        log.info("hit1 == hit3: {}", hit1.equals(hit3));
    }

    @Test
    void testToString() {
        log.info("Метод toString");

        EndpointHit hit = EndpointHit.builder()
                .id(99L)
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        String toString = hit.toString();

        assertThat(toString).isNotNull();
        assertThat(toString).contains("test-app");
        assertThat(toString).contains("/test");

        log.info("toString: {}", toString);
    }
}