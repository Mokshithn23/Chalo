package com.chalo.controller;

import com.chalo.model.Adventure;
import com.chalo.model.JoinRequest;
import com.chalo.model.User;
import com.chalo.repository.AdventureRepository;
import com.chalo.repository.JoinRequestRepository;
import com.chalo.repository.UserRepository;
import com.chalo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "My Adventures" page: GET /my-adventures (templates/my-adventures.html).
 * Three sections:
 *   1. Adventures I HOST   (with pending-request badges and seats taken)
 *   2. Adventures I JOINED (accepted requests)
 *   3. My PENDING requests (waiting for a host's answer)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MyAdventuresController {

    private final AdventureRepository   adventureRepository;
    private final UserRepository        userRepository;
    private final JoinRequestRepository joinRequestRepository;

    /** Builds all three sections plus the per-adventure counters. */
    @GetMapping("/my-adventures")
    public String myAdventures(@AuthenticationPrincipal CustomUserDetails principal,
                               Model model) {

        // Placeholder object carrying only the logged-in user's id
        User currentUser = userRepository.getReferenceById(principal.getId());

        // Section 1: adventures hosted by me, latest date first
        List<Adventure> hostedAdventures = adventureRepository.findByHostOrderByAdventureDateDesc(currentUser);

        List<Long> adventureIds = hostedAdventures.stream().map(Adventure::getId).toList();
        log.debug("myAdventures → hostId={} adventureIds={}", principal.getId(), adventureIds);

        // Two lookup tables: adventureId -> number of pending / accepted requests.
        // (Adventures with 0 are absent; the template falls back to 0.)
        Map<Long, Long> pendingCounts = Collections.emptyMap();
        Map<Long, Long> hostedAcceptedCounts = Collections.emptyMap();
        if (!adventureIds.isEmpty()) {
            List<Object[]> pendingRows = joinRequestRepository.countPendingByAdventureIds(adventureIds);
            log.debug("myAdventures → countPendingByAdventureIds raw rows={}", pendingRows.size());
            for (Object[] countRow : pendingRows) {
                log.debug("  adventureId={} pendingCount={}", countRow[0], countRow[1]);
            }

            // Turn the raw [adventureId, count] rows into a Map<adventureId, count>
            pendingCounts = pendingRows.stream()
                    .collect(Collectors.toMap(
                            countRow -> (Long) countRow[0],
                            countRow -> (Long) countRow[1]
                    ));

            List<Object[]> acceptedRows = joinRequestRepository.countAcceptedByAdventureIds(adventureIds);
            hostedAcceptedCounts = acceptedRows.stream()
                    .collect(Collectors.toMap(
                            countRow -> (Long) countRow[0],
                            countRow -> (Long) countRow[1]
                    ));
        }

        log.debug("myAdventures → pendingCounts map={}", pendingCounts);

        // ── Requester sections (adventures current user joined / applied to) ──
        User requester = currentUser; // same User object — host is just a proxy reference
        List<JoinRequest> joinedRequests  = joinRequestRepository
                .findAcceptedByRequesterWithAdventureAndHost(requester);
        List<JoinRequest> pendingRequests = joinRequestRepository
                .findPendingByRequesterWithAdventureAndHost(requester);

        // TEMP DEBUG — remove after confirming root cause (badge vs card mismatch)
        log.debug("joinedRequests.size()={}", joinedRequests.size());
        for (JoinRequest joinRequest : joinedRequests) {
            log.debug("  jr.id={} adventure.id={} adventure.title={}",
                    joinRequest.getId(), joinRequest.getAdventure().getId(), joinRequest.getAdventure().getTitle());
        }

        // Accepted count per joined adventure (one batch query, no N+1).
        // Template computes slotsLeft = adventure.maxParticipants - acceptedCount.
        Map<Long, Long> joinedAcceptedCounts = Collections.emptyMap();
        if (!joinedRequests.isEmpty()) {
            List<Long> joinedAdventureIds = joinedRequests.stream()
                    .map(joinRequest -> joinRequest.getAdventure().getId()).toList();
            List<Object[]> acceptedRows = joinRequestRepository
                    .countAcceptedByAdventureIds(joinedAdventureIds);
            joinedAcceptedCounts = acceptedRows.stream().collect(Collectors.toMap(
                    countRow -> (Long) countRow[0],
                    countRow -> (Long) countRow[1]
            ));
        }

        // Hand everything to my-adventures.html
        model.addAttribute("hostedAdventures",      hostedAdventures);
        model.addAttribute("pendingCounts",          pendingCounts);
        model.addAttribute("hostedAcceptedCounts",   hostedAcceptedCounts);
        model.addAttribute("joinedRequests",       joinedRequests);
        model.addAttribute("joinedAcceptedCounts", joinedAcceptedCounts);
        model.addAttribute("pendingRequests",      pendingRequests);
        model.addAttribute("userName",             principal.getName());

        return "my-adventures";
    }
}
