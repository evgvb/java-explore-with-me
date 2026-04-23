package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final LocalDateTime STATS_START = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final EventMapper eventMapper;

    // приватные

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        log.info("Создание события пользователем id={}", userId);

        User user = getUserById(userId);

        Category category = getCategoryById(dto.getCategory());

        // дата начала не может быть раньше, чем через 2 часа от текущего момента
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата начала события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toEntity(dto, user, category);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0L);

        Event saved = eventRepository.save(event);
        log.debug("Событие создано с id={}", saved.getId());

        // статистику в DTO  (пока views = 0, confirmedRequests = 0)
        return enrichWithStats(eventMapper.toFullDto(saved));
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        log.info("Получение событий пользователя id={}, from={}, size={}", userId, from, size);

        getUserById(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);
        List<EventShortDto> dtos = events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
        return enrichShortWithStats(dtos);
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        log.info("Пользователь id={} получает своё событие id={}", userId, eventId);
        Event event = getEventByUserIds(eventId, userId);
        return enrichWithStats(eventMapper.toFullDto(event));
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest dto) {
        log.info("Пользователь id={} обновляет событие id={}", userId, eventId);
        Event event = getEventByUserIds(eventId, userId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие: id=" + eventId);
        }

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата начала должна быть не ранее чем через 2 часа от текущего момента");
        }

        Category category = null;
        if (dto.getCategory() != null) {
            category = getCategoryById(dto.getCategory());
        }

        eventMapper.updateEntity(event, dto, category);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new BadRequestException("Недопустимое действие над статусом");
            }
        }

        Event updated = eventRepository.save(event);
        log.debug("Событие обновлено, новый статус: {}", updated.getState());
        return enrichWithStats(eventMapper.toFullDto(updated));
    }

    // публичные

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, int from, int size) {
        log.info("Публичный поиск событий: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, " +
                        "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllPublishedWithFilters(text, categories, paid, rangeStart, rangeEnd, pageable);

        // Фильтрация по onlyAvailable (если true, оставляем только события с непереполненным лимитом)
        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            requestRepository.countConfirmedByEventId(e.getId()) < e.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        List<EventShortDto> dtos = events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
        dtos = enrichShortWithStats(dtos);

        // Сортировка
        if ("VIEWS".equals(sort)) {
            dtos.sort(Comparator.comparing(EventShortDto::getViews, Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            // По умолчанию сортировка по дате события (EVENT_DATE)
            dtos.sort(Comparator.comparing(EventShortDto::getEventDate));
        }
        return dtos;
    }

    @Override
    public EventFullDto getPublicEvent(Long id) {
        log.info("Получение публичного события id={}", id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        return enrichWithStats(eventMapper.toFullDto(event));
    }

    // Администратороские

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        log.info("Административный поиск событий: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}",
                users, states, categories, rangeStart, rangeEnd);
        Pageable pageable = PageRequest.of(from / size, size);
        List<EventState> eventStates = (states == null) ? null :
                states.stream().map(EventState::valueOf).collect(Collectors.toList());

        List<Event> events = eventRepository.findAllByAdminFilters(users, eventStates, categories, rangeStart, rangeEnd, pageable);
        return events.stream()
                .map(eventMapper::toFullDto)
                .map(this::enrichWithStats)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        log.info("Администратор обновляет событие id={}", eventId);
        Event event = getEventById(eventId);

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
        }

        Category category = null;
        if (dto.getCategory() != null) {
            category = getCategoryById(dto.getCategory());
        }

        eventMapper.updateEntity(event, dto, category);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Событие можно опубликовать только в состоянии ожидания");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Опубликованное событие нельзя отклонить");
                    }
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new BadRequestException("Недопустимое действие над статусом");
            }
        }

        Event updated = eventRepository.save(event);
        log.debug("Событие обновлено администратором, статус: {}", updated.getState());
        return enrichWithStats(eventMapper.toFullDto(updated));
    }

    // Статистика

    private EventFullDto enrichWithStats(EventFullDto dto) {
        if (dto == null) return null;

        ViewStats stats = statsClient.getStatsForUri(
                STATS_START, // очень давно
                LocalDateTime.now(),
                dto.getUri(),
                true);
        dto.setViews(stats != null ? stats.getHits() : 0L);

        log.debug("Статистика для {}: hits={}", dto.getUri(), stats != null ? stats.getHits() : 0);

        Long confirmed = requestRepository.countConfirmedByEventId(dto.getId());
        dto.setConfirmedRequests(confirmed != null ? confirmed : 0L);

        return dto;
    }

    private List<EventShortDto> enrichShortWithStats(List<EventShortDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return dtos;

        // Собираем все uri для запроса статистики
        List<String> uris = dtos.stream()
                .map(EventShortDto::getUri)
                .collect(Collectors.toList());

        // Запрашиваем статистику для всех uri за всё время
        List<ViewStats> statsList = statsClient.getStats(
                STATS_START,
                LocalDateTime.now(),
                uris,
                false);


        // Преобразуем в Map<uri, hits>
        Map<String, Long> hitsMap = statsList.stream()
                .collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits));

        // Для каждого DTO проставляем views и confirmedRequests
        for (EventShortDto dto : dtos) {
            dto.setViews(hitsMap.getOrDefault(dto.getUri(), 0L));
            Long confirmed = requestRepository.countConfirmedByEventId(dto.getId());
            dto.setConfirmedRequests(confirmed != null ? confirmed : 0L);
        }
        return dtos;
    }

    private Event getEventByUserIds(Long eventId, Long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие id=%d не найдено или не принадлежит пользователю id=%d", eventId, userId)));
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: id=" + id));
    }

    private Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория не найдена: id=" + id));
    }

    private Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие не найдено: id=" + id));
    }
}