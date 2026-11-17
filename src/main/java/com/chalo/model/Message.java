package com.chalo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One chat message written by a user inside a Chat. Maps to table "messages".
 *
 * Created by CrewChatService.postMessage(); listed by MessageRepository and
 * displayed as bubbles in chat/crew-chat.html.
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"chat", "sender"})
public class Message {

    /** Primary key (auto-increment). Newer messages always have a bigger id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The chat room this message was posted in (foreign key chat_id). */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    /** The author (foreign key sender_id -> users.id). */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /** The text (max 2000 characters, enforced in CrewChatService.postMessage). */
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** When it was sent (automatic). */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
