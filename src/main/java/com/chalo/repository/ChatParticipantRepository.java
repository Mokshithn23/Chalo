package com.chalo.repository;

import com.chalo.model.Chat;
import com.chalo.model.ChatParticipant;
import com.chalo.model.ChatParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Database access for "chat_participants" (members of a chat).
 * The key type is ChatParticipantId because the primary key has two columns.
 * NOTE: neither method is used yet (see ChatParticipant for why).
 */
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {

    // ── Duplicate-add guard ───────────────────────────────────────────────────
    /** true if this user is already a member of this chat (prevents duplicates). */
    boolean existsByIdChatIdAndIdUserId(Long chatId, Long userId);

    // ── Chat room: show participant list with user names ──────────────────────
    /** All members of a chat, with their User rows loaded at once (JOIN FETCH avoids extra queries). */
    @Query("""
            SELECT cp FROM ChatParticipant cp
            JOIN FETCH cp.user
            WHERE cp.chat = :chat
            """)
    List<ChatParticipant> findByChatWithUser(@Param("chat") Chat chat);
}
