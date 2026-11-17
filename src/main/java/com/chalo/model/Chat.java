package com.chalo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The group chat room of ONE adventure ("Crew Chat"). Maps to table "chats".
 *
 * Exactly one chat per adventure (adventure_id is UNIQUE). It is created lazily by
 * CrewChatService.getOrCreateChat() the first time the host or an accepted member
 * opens the chat page.
 */
@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"adventure", "participants", "messages"})
public class Chat {

    /** Primary key (auto-increment). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The adventure this chat belongs to. unique=true -> one chat per adventure. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adventure_id", nullable = false, unique = true)
    private Adventure adventure;

    /** When the chat room was created (automatic). */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Members list (ChatParticipant rows). Not used for permission checks in this version. */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

    /** All messages, oldest first. */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
}
