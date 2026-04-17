package ru.practicum.ewm.event.service;

import ru.practicum.ewm.event.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    // Приватные методы
    EventFullDto createEvent(Long userId, NewEventDto dto);
    List<EventShortDto> getUserEvents(Long userId, int from, int size);
    EventFullDto getEventByUser(Long userId, Long eventId);
    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest dto);

    // Публичные методы
    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort, int from, int size);
    EventFullDto getPublicEvent(Long id);

    // Административные методы
    List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);
    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto);
}