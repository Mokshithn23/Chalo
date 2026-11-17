package com.chalo.controller;

import com.chalo.model.Adventure;
import com.chalo.model.AdventureStatus;
import com.chalo.model.Chat;
import com.chalo.model.JoinRequestStatus;
import com.chalo.model.Message;
import com.chalo.repository.AdventureRepository;
import com.chalo.repository.JoinRequestRepository;
import com.chalo.repository.MessageRepository;
import com.chalo.security.CustomUserDetails;
import com.chalo.service.CrewChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * The group chat ("Crew Chat") of one adventure.
 *
 * URLs handled here (both require login):
 *   GET  /adventures/{id}/chat           show the chat room (chat/crew-chat.html)
 *   POST /adventures/{id}/chat/messages  send a message, then reload the chat
 *
 * Who may enter? The host and every participant whose request was ACCEPTED
 * (rule in CrewChatService.canAccess). Everybody else gets "403 Forbidden".
 * The chat is available only while the adventure is PUBLISHED or COMPLETED.
 *
 * NOTE: the page does NOT refresh itself; you see new messages after sending one or
 * reloading the page. (The planned 5-second auto-refresh is not implemented.)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CrewChatController {

    private final AdventureRepository   adventureRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final MessageRepository     messageRepository;
    private final CrewChatService       crewChatService;

    // ── View Crew Chat ────────────────────────────────────────────────────────
    // findWithDetailById eagerly loads host via @EntityGraph — required for
    // canAccess() and the hostId model attribute without an open session.

    /** GET /adventures/{id}/chat - checks permission, loads messages, renders the chat. */
    @GetMapping("/adventures/{id}/chat")
    public String showChat(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {

        Adventure adventure = adventureRepository.findWithDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Permission check 1: must be host or accepted member
        if (!crewChatService.canAccess(adventure, principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Permission check 2: chat is closed for cancelled / draft adventures
        AdventureStatus status = adventure.getStatus();
        if (status != AdventureStatus.PUBLISHED && status != AdventureStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Chat          chat             = crewChatService.getOrCreateChat(adventure.getId());
        List<Message> messages         = messageRepository.findByChatIdWithSender(chat.getId());
        long          acceptedCount    = joinRequestRepository.countByAdventureAndStatus(
                                                 adventure, JoinRequestStatus.ACCEPTED);

        log.debug("showChat → adventureId={} userId={} messageCount={}",
                id, principal.getId(), messages.size());

        model.addAttribute("adventure",        adventure);
        model.addAttribute("messages",         messages);
        model.addAttribute("participantCount", acceptedCount + 1);   // +1 for host
        model.addAttribute("hostId",           adventure.getHost().getId());
        model.addAttribute("currentUserId",    principal.getId());
        model.addAttribute("isHost",           adventure.getHost().getId().equals(principal.getId()));
        model.addAttribute("userName",         principal.getName());

        return "chat/crew-chat";
    }

    // ── Post Message ─────────────────────────────────────────────────────────
    // No @Transactional at controller level — each service method owns its own
    // transaction. Blank content is rejected before reaching the service.

    /** POST /adventures/{id}/chat/messages - saves the typed message and redisplays the chat. */
    @PostMapping("/adventures/{id}/chat/messages")
    public String postMessage(@PathVariable Long id,
                              @RequestParam(required = false) String content,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {

        Adventure adventure = adventureRepository.findWithDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Same two permission checks as showChat (never trust that the form was only
        // reachable from the chat page)
        if (!crewChatService.canAccess(adventure, principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        AdventureStatus status = adventure.getStatus();
        if (status != AdventureStatus.PUBLISHED && status != AdventureStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Empty / whitespace-only messages are not stored
        if (content == null || content.isBlank()) {
            redirectAttributes.addFlashAttribute("chatError", "Message cannot be empty.");
            return "redirect:/adventures/" + id + "/chat";
        }

        Chat chat = crewChatService.getOrCreateChat(adventure.getId());
        crewChatService.postMessage(chat.getId(), principal.getId(), content);

        log.debug("postMessage → adventureId={} senderId={}", id, principal.getId());
        // Post/Redirect/Get: redirecting stops the browser re-sending the message on refresh
        return "redirect:/adventures/" + id + "/chat";
    }
}
