package com.taskhive.service;

import com.taskhive.dto.CommentDto;
import com.taskhive.model.Comment;
import com.taskhive.repository.CommentRepository;
import com.taskhive.repository.TaskRepository;
import com.taskhive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public Comment create(CommentDto dto, Long taskId, String email) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var comment = Comment.builder()
                .task(task)
                .user(user)
                .content(dto.getContent())
                .build();

        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> getByTaskId(Long taskId) {
        return commentRepository.findByTaskTaskIdOrderByCreatedAtDesc(taskId);
    }

    @Transactional
    public void softDelete(Long commentId, String email) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
    }
}
