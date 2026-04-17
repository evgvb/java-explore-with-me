package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestMapper mapper;

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.info("Пользователь id={} подаёт запрос на участие в событии id={}", userId, eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        // Проверки
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать запрос на участие");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (event.getParticipantLimit() > 0 &&
                requestRepository.countConfirmedByEventId(eventId) >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит запросов на участие");
        }
        if (requestRepository.findAllByRequesterId(userId).stream()
                .anyMatch(r -> r.getEvent().getId().equals(eventId))) {
            throw new ConflictException("Повторный запрос запрещён");
        }

        RequestStatus status = event.getRequestModeration() && event.getParticipantLimit() > 0
                ? RequestStatus.PENDING
                : RequestStatus.CONFIRMED;

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(status)
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        log.debug("Запрос создан с id={}, статус={}", saved.getId(), status);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Пользователь id={} отменяет запрос id={}", userId, requestId);
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));
        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        log.debug("Запрос отменён");
        return mapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение запросов пользователя id={}", userId);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение запросов на событие id={} для пользователя id={}", eventId, userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не принадлежит пользователю"));
        return requestRepository.findAllByEventId(eventId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.info("Изменение статуса запросов для события id={}, пользователь id={}, запросы={}, статус={}",
                eventId, userId, updateRequest.getRequestIds(), updateRequest.getStatus());

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не принадлежит пользователю"));

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new BadRequestException("Для события не требуется подтверждение заявок");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());
        // проверяем, что все запросы относятся к этому событию и имеют статус PENDING
        for (ParticipationRequest req : requests) {
            if (!req.getEvent().getId().equals(eventId)) {
                throw new BadRequestException("Запрос не относится к данному событию");
            }
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new BadRequestException("Статус можно изменить только у заявок в состоянии ожидания");
            }
        }

        long confirmedCount = requestRepository.countConfirmedByEventId(eventId);
        long availableSlots = event.getParticipantLimit() - confirmedCount;

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
            for (ParticipationRequest req : requests) {
                if (availableSlots > 0) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(mapper.toDto(req));
                    availableSlots--;
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(mapper.toDto(req));
                }
            }
        } else if (updateRequest.getStatus() == RequestStatus.REJECTED) {
            for (ParticipationRequest req : requests) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toDto(req));
            }
        } else {
            throw new BadRequestException("Недопустимый статус для обновления");
        }

        requestRepository.saveAll(requests);
        log.debug("Обновлено: подтверждено {}, отклонено {}", confirmed.size(), rejected.size());
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }
}