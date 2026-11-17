package com.chalo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * An in-app message for ONE user (the bell icon in the header). Maps to "notifications".
 *
 * Created by NotificationService.notifyUser(); listed on /notifications.
 *
 * NOTE: this table is NOT in schema.sql. It is created automatically by Hibernate
 * (spring.jpa.hibernate.ddl-auto=update) when the application starts.
 */
@Entity
// Two database indexes make the two most common lookups fast:
//   1. "all notifications of a user, newest first"
//   2. "how many unread notifications does this user have" (bell badge on every page)
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_notif_user_read",    columnList = "user_id, is_read")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class Notification {

    /** Primary key (auto-increment). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The person who receives this notification (foreign key user_id). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Short headline, e.g. "Request Accepted". */
    @Column(nullable = false, length = 100)
    private String title;

    /** Longer sentence, e.g. 'Your request to join "Sunrise Trek" has been accepted.' */
    @Column(nullable = false, length = 500)
    private String message;

    /** Category (see NotificationType); decides the icon in the UI. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // Field named 'read' so Lombok generates isRead() / setRead() — the standard
    // JavaBeans boolean accessor pattern expected by Spring and Thymeleaf.
    /** false = unread (counts towards the bell badge), true = already seen. Column "is_read". */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** Where to go when the notification is clicked, e.g. "/adventures/5". May be empty. */
    @Column(length = 500)
    private String link;

    /** When the notification was created (automatic). Shown as "5m ago". */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
