package ru.practicum.ewm.comment.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.comment.model.Comment;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId AND c.deleted = false ORDER BY c.created DESC")
    List<Comment> findAllByEventId(@Param("eventId") Long eventId, Pageable pageable);

    // поиск комментария автором
    Optional<Comment> findByIdAndAuthorId(Long commentId, Long authorId);
}