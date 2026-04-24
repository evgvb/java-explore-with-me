package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.AccessDeniedException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        log.debug("Получение комментариев события id={}", eventId);
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        return comments.stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Пользователь id={} добавляет комментарий к событию id={}", userId, eventId);

        User author = getUserById(userId);
        Event event = gerEventById(eventId);

        // Комментировать можно только опубликованные события
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = Comment.builder()
                .text(newCommentDto.getText())
                .event(event)
                .author(author)
                .created(LocalDateTime.now())
                .deleted(false)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Комментарий сохранён с id={}", saved.getId());
        return commentMapper.toCommentDto(saved);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto updateCommentDto) {
        log.info("Пользователь id={} обновляет комментарий id={}", userId, commentId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId).orElseThrow(() -> new NotFoundException(
                String.format("Комментарий id=%d не найден или не принадлежит пользователю id=%d", commentId, userId)));

        if (comment.getDeleted()) {
            throw new ConflictException("Нельзя редактировать удалённый комментарий");
        }

        comment.setText(updateCommentDto.getText());
        comment.setEdited(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);
        return commentMapper.toCommentDto(saved);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Пользователь id={} удаляет комментарий id={}", userId, commentId);

        Comment comment = getCommentById(commentId);

        // Проверка прав: только автор или админ (для админа отдельный метод)
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Вы не можете удалить чужой комментарий");
        }

        // Soft delete
        comment.setDeleted(true);
        commentRepository.save(comment);
        log.info("Комментарий id={} помечен удалённым", commentId);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Администратор удаляет комментарий id={}", commentId);
        Comment comment = getCommentById(commentId);

        comment.setDeleted(true);
        commentRepository.save(comment);
        log.info("Комментарий id={} удалён администратором", commentId);
    }

    private User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Пользователь не найден: id=" + id));
    }

    private Event gerEventById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Событие не найдено: id=" + id));
    }

    private Comment getCommentById(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> new NotFoundException("Комментарий не найден: id=" + id));
    }
}