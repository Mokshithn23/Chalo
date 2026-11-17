package com.chalo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * "User X is a member of chat Y". Maps to table "chat_participants".
 *
 * This is a JUNCTION entity between Chat and User with its own extra column (joined_at),
 * which is why it is a class and not a simple @ManyToMany. Its primary key is the PAIR
 * (chat_id, user_id) = ChatParticipantId.
 *
 * NOTE: in the current version nothing creates these rows. Access to the chat is decided
 * directly from join_requests (see CrewChatService.canAccess). The table is kept for a
 * future "member list" feature.
 */
@Entity
@Table(name = "chat_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"chat", "user"})
public class ChatParticipant {

    /** Composite primary key (chatId + userId). */
    @EmbeddedId
    private ChatParticipantId id;

    /**
     * The chat. @MapsId("chatId") says: "the chat_id column of the key IS this relationship",
     * so JPA fills id.chatId from this object automatically.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private Chat chat;

    /** The member. @MapsId("userId") fills id.userId automatically. */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    /** When the user joined the chat (automatic). */
    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
