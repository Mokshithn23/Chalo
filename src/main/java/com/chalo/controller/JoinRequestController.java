package com.chalo.controller;

import com.chalo.model.Adventure;
import com.chalo.model.AdventureStatus;
import com.chalo.model.JoinRequest;
import com.chalo.model.JoinRequestStatus;
import com.chalo.model.NotificationType;
import com.chalo.repository.JoinRequestRepository;
import com.chalo.security.CustomUserDetails;
import com.chalo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The HOST's decision on a join request: Accept or Reject.
 * Both buttons live on adventures/requests.html and POST to:
 *   POST /requests/{id}/accept
 *   POST /requests/{id}/reject
 * ({id} = the JoinRequest id, not the adventure id.)
 * After the action the host is sent back to /adventures/{adventureId}/requests.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestRepository joinRequestRepository;
    private final NotificationService   notificationService;

    // ── Accept ────────────────────────────────────────────────────────────────
    // Only the adventure's host may accept. The JOIN FETCH query avoids lazy-load
    // issues when reading adventure.host outside of open-session-in-view.

    /** Accepts a pending request (if the caller is the host and there is a free seat). */
    @PostMapping("/requests/{id}/accept")
    @Transactional
    public String accept(@PathVariable Long id,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {

        // Load request + adventure + host in one query
        JoinRequest joinRequest = joinRequestRepository.findByIdWithAdventureAndHost(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Security: only the host of the adventure may decide
        if (!joinRequest.getAdventure().getHost().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Adventure adventure = joinRequest.getAdventure();

        // Rule 1: no decisions on cancelled adventures
        if (adventure.getStatus() == AdventureStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("actionError", "Cannot accept — this adventure has been cancelled.");
            return "redirect:/adventures/" + adventure.getId() + "/requests";
        }

        // Rule 2: idempotent - accepting twice does nothing harmful
        if (joinRequest.getStatus() == JoinRequestStatus.ACCEPTED) {
            redirectAttributes.addFlashAttribute("actionSuccess", "Request already accepted.");
            return "redirect:/adventures/" + adventure.getId() + "/requests";
        }

        // Rule 3: there must be a free seat
        long acceptedCount = joinRequestRepository.countByAdventureAndStatus(adventure, JoinRequestStatus.ACCEPTED);
        if (acceptedCount >= adventure.getMaxParticipants()) {
            log.warn("accept: adventure {} already full ({}/{})", adventure.getId(), acceptedCount, adventure.getMaxParticipants());
            redirectAttributes.addFlashAttribute("actionError",
                    "Cannot accept — this adventure is already full (" + acceptedCount + "/" + adventure.getMaxParticipants() + " slots taken).");
            return "redirect:/adventures/" + adventure.getId() + "/requests";
        }

        // Dirty-checking emits the UPDATE on transaction commit — no explicit save needed.
        joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
        log.info("accept: request {} accepted for adventure {} ({}/{})",
                id, adventure.getId(), acceptedCount + 1, adventure.getMaxParticipants());

        // Tell the traveller the good news (bell icon)
        notificationService.notifyUser(
                joinRequest.getRequester().getId(),
                "Request Accepted",
                "Your request to join \"" + adventure.getTitle() + "\" has been accepted.",
                NotificationType.REQUEST_ACCEPTED,
                "/adventures/" + adventure.getId()
        );

        redirectAttributes.addFlashAttribute("actionSuccess", "Request accepted.");
        return "redirect:/adventures/" + adventure.getId() + "/requests";
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    /** Rejects a request (not allowed once it is already accepted). */
    @PostMapping("/requests/{id}/reject")
    @Transactional
    public String reject(@PathVariable Long id,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {

        JoinRequest joinRequest = joinRequestRepository.findByIdWithAdventureAndHost(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Security: only the host may decide
        if (!joinRequest.getAdventure().getHost().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Adventure rejectedAdventure = joinRequest.getAdventure();

        if (rejectedAdventure.getStatus() == AdventureStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("actionError", "Cannot reject — this adventure has been cancelled.");
            return "redirect:/adventures/" + rejectedAdventure.getId() + "/requests";
        }

        // A participant who is already in the crew cannot be silently removed
        if (joinRequest.getStatus() == JoinRequestStatus.ACCEPTED) {
            redirectAttributes.addFlashAttribute("actionError", "Cannot reject an already-accepted participant.");
            return "redirect:/adventures/" + rejectedAdventure.getId() + "/requests";
        }

        joinRequest.setStatus(JoinRequestStatus.REJECTED);

        notificationService.notifyUser(
                joinRequest.getRequester().getId(),
                "Request Rejected",
                "Your request to join \"" + rejectedAdventure.getTitle() + "\" was not accepted.",
                NotificationType.REQUEST_REJECTED,
                "/adventures/" + rejectedAdventure.getId()
        );

        redirectAttributes.addFlashAttribute("actionSuccess", "Request rejected.");
        return "redirect:/adventures/" + rejectedAdventure.getId() + "/requests";
    }
}
