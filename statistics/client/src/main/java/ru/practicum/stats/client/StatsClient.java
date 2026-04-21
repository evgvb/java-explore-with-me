package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

    // формат даты и времени
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // URL сервера статистики
    private final String serverUrl;

    // RestTemplate для выполнения HTTP-запросов
    private final RestTemplate restTemplate;

    // serverUrl URL сервера статистики (из application.properties)
    // restTemplateBuilder билдер для создания RestTemplate
    public StatsClient(
            @Value("${stats-server.url:http://localhost:9090}") String serverUrl,
            RestTemplateBuilder restTemplateBuilder) {
        this.serverUrl = serverUrl;
        this.restTemplate = restTemplateBuilder
                .build();
        log.info("Запущен StatsClient с URL сервера: {}", serverUrl);
    }

    public EndpointHit hit(EndpointHit hit) {
        log.debug("Отправка запроса на регистрацию обращения: app={}, uri={}, ip={}",
                hit.getApp(), hit.getUri(), hit.getIp());

        try {

            String url = serverUrl + "/hit";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(hit, headers);

            ResponseEntity<EndpointHit> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    EndpointHit.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Обращение успешно зарегистрировано с ID: {}", response.getBody().getId());
                return response.getBody();
            } else {
                log.warn("Неожиданный статус ответа при регистрации обращения: {}", response.getStatusCode());
                return hit;
            }
        } catch (Exception e) {
            log.error("Ошибка при регистрации обращения к серверу статистики: {}", e.getMessage(), e);
            return hit;
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.debug("Запрос статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(serverUrl + "/stats")
                    .queryParam("start", start.format(FORMATTER))
                    .queryParam("end", end.format(FORMATTER))
                    .queryParam("unique", unique != null ? unique : false);


            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    builder.queryParam("uris", uri);
                }
            }

            String url = builder.build(false).toUriString();

            log.debug("Сформированный URL для запроса статистики: {}", url);

            ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(url, ViewStats[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStats> stats = Arrays.asList(response.getBody());
                log.debug("Получена статистика: {} записей", stats.size());
                return stats;
            } else {
                log.warn("Неожиданный статус ответа при получении статистики: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, Boolean unique) {
        return getStats(start, end, null, unique);
    }

    public ViewStats getStatsForUri(LocalDateTime start, LocalDateTime end, String uri, Boolean unique) {
        List<ViewStats> stats = getStats(start, end, List.of(uri), unique);
        if (stats.isEmpty()) {
            return null;
        }

        return stats.get(0);
    }
}