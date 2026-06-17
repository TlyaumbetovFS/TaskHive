package com.taskhive.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "task_tags", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"task_id", "tag_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTag {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_tag_seq")
    @SequenceGenerator(name = "task_tag_seq", sequenceName = "task_tags_seq", allocationSize = 50)
    @Column(name = "task_tag_id")
    private Long taskTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false, updatable = false)
    private Tag tag;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
