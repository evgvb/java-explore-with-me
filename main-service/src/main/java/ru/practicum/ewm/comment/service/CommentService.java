package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;

import java.util.List;

public interface CommentService {

    // публичное
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    // приват
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto updatedCommentDto);

    void deleteComment(Long userId, Long commentId);

    // администратор
    void deleteCommentByAdmin(Long commentId);
}