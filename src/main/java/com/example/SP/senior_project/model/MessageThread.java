package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "message_threads",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id","user2_id"})
)
@Getter @Setter
public class MessageThread extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NOTE: match your existing DB columns user1_id / user2_id
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user1_id", nullable = false)
    private RoomFinder user1;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user2_id", nullable = false)
    private RoomFinder user2;
}
