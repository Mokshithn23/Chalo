package com.chalo.service;

import com.chalo.model.Adventure;
import com.chalo.model.Chat;
import com.chalo.model.JoinRequestStatus;
import com.chalo.model.Message;
import com.chalo.model.User;
import com.chalo.repository.AdventureRepository;
import com.chalo.repository.ChatRepository;
import com.chalo.repository.JoinRequestRepository;
import com.chalo.repository.MessageRepository;
import com.chalo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules of the "Crew Chat" (group chat of one adventure).
 *
 * Used by CrewChatController. Three jobs:
 *   1. canAccess()        -> who is allowed to read / write in the chat?
 *   2. getOrCreateChat()  -> make sure the chat room row exists.
 *   3. postMessage()      -> save a new message.
 *
 * @Slf4j (Lombok) creates a ready-to-use "log" object for printing debug lines.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrewChatService {

    // Each of these gives access to one database table (injected by Spring).
    private final ChatRepository        chatRepository;
    private final MessageRepository     messageRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final AdventureRepository   adventureRepository;
    private final UserRepository        userRepository;

    // ── Authorization ─────────────────────────────────────────────────────────
    // Host check is a simple ID comparison (no DB hit; host is eagerly loaded
    // by findWithDetailById before this is called).
    // Participant check queries JoinRequest directly — ChatParticipant is unused
    // for authorization in MVP.

    /**
     * Decides whether a user may open the chat of an adventure.
     * Allowed = the host, OR someone whose join request was ACCEPTED.
     *
     * @param adventure the adventure whose chat is requested (host must already be loaded)
     * @param userId    id of the logged-in user
     * @return true if the user belongs to the crew
     */
    public boolean canAccess(Adventure adventure, Long userId) {
        // Case 1: the user is the host
        if (adventure.getHost().getId().equals(userId)) {
            return true;
        }
        // Case 2: the user has an ACCEPTED join request for this adventure
        return joinRequestRepository.existsByAdventureIdAndRequesterIdAndStatus(
                adventure.getId(), userId, JoinRequestStatus.ACCEPTED);
    }

    // ── Chat creation ─────────────────────────────────────────────────────────
    // Lazy: created on first access, not on join-request acceptance.
    // getReferenceById gives a proxy with only the PK set — sufficient for the FK.

    /**
     * Returns the chat of an adventure, creating it the first time it is needed.
     *
     * @param adventureId id of the adventure
     * @return the existing or newly created Chat
     */
    @Transactional
    public Chat getOrCreateChat(Long adventureId) {
        return chatRepository.findByAdventureId(adventureId)
                .orElseGet(() -> {
                    log.debug("getOrCreateChat → no chat yet, creating for adventure {}", adventureId);
                    // A lightweight placeholder object: only the id is known, no SELECT is executed.
                    Adventure adventureReference = adventureRepository.getReferenceById(adventureId);
                    return chatRepository.save(Chat.builder().adventure(adventureReference).build());
                });
    }

    // ── Message persistence ───────────────────────────────────────────────────
    // Content is trimmed and capped server-side; the @NotBlank constraint on
    // Message.content means a blank string would fail validation before save.

    /**
     * Saves one chat message.
     *
     * @param chatId   chat room id
     * @param senderId id of the user who wrote it
     * @param content  the raw text typed by the user
     */
    @Transactional
    public void postMessage(Long chatId, Long senderId, String content) {
        // Remove leading/trailing spaces
        String trimmedContent = content.trim();
        // Safety limit: never store more than 2000 characters
        if (trimmedContent.length() > 2000) {
            trimmedContent = trimmedContent.substring(0, 2000);
        }
        // Placeholders that carry only the ids (enough to fill the foreign-key columns)
        Chat chatReference   = chatRepository.getReferenceById(chatId);
        User senderReference = userRepository.getReferenceById(senderId);
        messageRepository.save(Message.builder()
                .chat(chatReference)
                .sender(senderReference)
                .content(trimmedContent)
                .build());
        log.debug("postMessage → chatId={} senderId={} chars={}", chatId, senderId, trimmedContent.length());
    }
}
