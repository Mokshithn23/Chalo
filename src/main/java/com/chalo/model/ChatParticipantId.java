package com.chalo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/**
 * COMPOSITE PRIMARY KEY for the chat_participants table.
 *
 * Why is it needed?
 *   chat_participants has no single "id" column. A row is uniquely identified by the
 *   PAIR (chat_id, user_id). JPA models such a two-column key as a small separate class
 *   marked @Embeddable and used as @EmbeddedId inside ChatParticipant.
 *
 * Rules JPA enforces for such a class:
 *   - must implement Serializable
 *   - must have equals() and hashCode()  (Lombok's @EqualsAndHashCode writes them)
 *   - must have a no-argument constructor (Lombok's @NoArgsConstructor)
 *
 * Lombok annotations used (they generate boring code at compile time):
 *   @Getter / @Setter  -> getChatId(), setChatId(), getUserId(), setUserId()
 *   @NoArgsConstructor -> new ChatParticipantId()
 *   @AllArgsConstructor-> new ChatParticipantId(chatId, userId)
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatParticipantId implements Serializable {

    /** Part 1 of the key: the id of the chat (column chat_id). */
    @Column(name = "chat_id")
    private Long chatId;

    /** Part 2 of the key: the id of the user (column user_id). */
    @Column(name = "user_id")
    private Long userId;
}
