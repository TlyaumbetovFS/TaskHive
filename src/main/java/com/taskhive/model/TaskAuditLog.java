package com.taskhive.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "task_audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_audit_log_seq")
    @SequenceGenerator(name = "task_audit_log_seq", sequenceName = "task_audit_logs_seq", allocationSize = 50)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false, updatable = false)
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, updatable = false)
    private TaskChangeType changeType;

    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @CreatedDate
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;
}
