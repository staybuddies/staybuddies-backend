package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.constant.NoticeType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Getter @Setter
@Entity
@Table(name = "notices")
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private RoomFinder user;               // target recipient

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NoticeType type;

    // Optional context
    private Long threadId;                 // for MESSAGE
    private Long fromUserId;               // who triggered the notice
    private String fromName;

    @Column(length = 120)
    private String title;

    @Column(length = 500)
    private String body;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private boolean readFlag = false;
}
