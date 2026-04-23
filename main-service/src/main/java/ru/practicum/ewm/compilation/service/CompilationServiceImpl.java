package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper mapper;

    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto dto) {
        log.info("Добавление новой подборки: title={}", dto.getTitle());
        Set<Event> events = dto.getEvents() == null ? new HashSet<>()
                : new HashSet<>(eventRepository.findAllById(dto.getEvents()));
        Compilation compilation = mapper.toEntity(dto, events);
        Compilation saved = compilationRepository.save(compilation);
        log.debug("Подборка добавлена с id={}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки id={}", compId);

        getCompilation(compId);
        compilationRepository.deleteById(compId);
        log.debug("Подборка удалена");
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.info("Обновление подборки id={}", compId);

        Compilation compilation = getCompilationById(compId);

        if (request.getPinned() != null) compilation.setPinned(request.getPinned());
        if (request.getTitle() != null) compilation.setTitle(request.getTitle());
        if (request.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
            compilation.setEvents(events);
        }
        Compilation updated = compilationRepository.save(compilation);
        log.debug("Подборка обновлена");
        return mapper.toDto(updated);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение подборок: pinned={}, from={}, size={}", pinned, from, size);
        PageRequest page = PageRequest.of(from / size, size);
        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, page);
        } else {
            compilations = compilationRepository.findAll(page).getContent();
        }
        return compilations.stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        log.info("Получение подборки id={}", compId);
        Compilation compilation = getCompilationById(compId);
        return mapper.toDto(compilation);
    }

    private Compilation getCompilationById(Long id) {
        return compilationRepository.findById(id).orElseThrow(() -> new NotFoundException("Подборка не найдена: id=" + id));
    }
}