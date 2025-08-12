package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "messages")
public class Message extends AbstractAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MessageThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private RoomFinder sender;

    // <-- single text field mapped to the DB column "body"
    @Column(name = "body", nullable = false, length = 4000)
    private String content;

    @Column(name = "read_by_other", nullable = false)
    private boolean readByOther = false;
}
