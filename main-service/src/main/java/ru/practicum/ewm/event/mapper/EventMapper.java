package ru.practicum.ewm.event.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public Event toEntity(NewEventDto dto, User initiator, Category category) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .createdOn(null) // будет установлено в сервисе
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .initiator(initiator)
                .location(new Location(dto.getLocation().getLatitude(), dto.getLocation().getLongitude()))
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .state(null) // установится в сервисе
                .title(dto.getTitle())
                .build();
    }

    public EventShortDto toShortDto(Event event) {
        if (event == null) return null;
        CategoryDto categoryDto = categoryMapper.toDto(event.getCategory());
        UserShortDto initiatorDto = userMapper.toShortDto(event.getInitiator());
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryDto)
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .initiator(initiatorDto)
                .paid(event.getPaid())
                .title(event.getTitle())
                .uri("/events/" + event.getId())
                .build();
    }

    public EventFullDto toFullDto(Event event) {
        if (event == null) return null;
        CategoryDto categoryDto = categoryMapper.toDto(event.getCategory());
        UserShortDto initiatorDto = userMapper.toShortDto(event.getInitiator());
        LocationDto locationDto = new LocationDto(event.getLocation().getLatitude(), event.getLocation().getLongitude());
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryDto)
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(initiatorDto)
                .location(locationDto)
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .uri("/events/" + event.getId())
                .build();
    }

    public void updateEntity(Event event, UpdateEventUserRequest dto, Category category) {
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (category != null) event.setCategory(category);
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) {
            event.setLocation(new Location(dto.getLocation().getLatitude(), dto.getLocation().getLongitude()));
        }
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
    }

    public void updateEntity(Event event, UpdateEventAdminRequest dto, Category category) {
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (category != null) event.setCategory(category);
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) {
            event.setLocation(new Location(dto.getLocation().getLatitude(), dto.getLocation().getLongitude()));
        }
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
    }
}