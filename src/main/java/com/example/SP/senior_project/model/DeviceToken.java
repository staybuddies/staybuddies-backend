package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "device_tokens", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"token"})
})
@Getter @Setter
public class DeviceToken extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** owner */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RoomFinder user;

    /** raw FCM token */
    @Column(name = "token", nullable = false, length = 2048)
    private String token;

    /** optional: "WEB" | "IOS" | "ANDROID" */
    @Column(name = "platform", length = 32)
    private String platform;

    /** optional device id / browser fingerprint */
    @Column(name = "device_id", length = 128)
    private String deviceId;
}
