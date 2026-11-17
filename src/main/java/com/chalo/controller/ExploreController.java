package com.chalo.controller;

import com.chalo.model.Adventure;
import com.chalo.model.Tag;
import com.chalo.repository.AdventureRepository;
import com.chalo.repository.JoinRequestRepository;
import com.chalo.repository.TagRepository;
import com.chalo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The public search page: GET /explore (templates/explore.html).
 * Anybody - even guests - can search adventures by interests and location.
 */
@Controller
@RequiredArgsConstructor
public class ExploreController {

    // Search queries on adventures
    private final AdventureRepository   adventureRepository;
    // Resolves slugs to tags and lists all tags for the chips
    private final TagRepository         tagRepository;
    // Counts accepted participants to compute "spots left"
    private final JoinRequestRepository joinRequestRepository;

    // ── Explore Adventures — GET /explore ─────────────────────────────────────
    // Permitted for all users (including anonymous) per SecurityConfig.
    // Params arrive from the dashboard search form: ?tags=slug&tags=slug&location=...

    /**
     * @param tags      optional list of tag slugs ticked in the search form (?tags=trekking&tags=camping)
     * @param location  optional text to search for in the location name
     * @param principal the logged-in user, or null for a guest
     * @param model     data bag for the template
     */
    @GetMapping("/explore")
    public String explore(
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String location,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {

        // Work out which filters the visitor actually used
        boolean hasTags     = tags != null && !tags.isEmpty();
        boolean hasLocation = location != null && !location.isBlank();
        String  locationFilter = hasLocation ? location.trim() : null;

        // -1L never matches a real auto-increment PK — anonymous users see
        // all qualifying adventures; logged-in users skip their own.
        Long excludeHostId = (principal != null) ? principal.getId() : -1L;

        List<Tag>       selectedTags;
        List<Adventure> results;

        if (hasTags) {
            // Resolve slug params → Tag entities → IDs for the JPQL IN clause
            selectedTags = tagRepository.findBySlugIn(tags);
            List<Long> tagIds = selectedTags.stream().map(Tag::getId).toList();
            results = tagIds.isEmpty()
                    ? List.of()
                    : adventureRepository.search(tagIds, locationFilter, excludeHostId);
        } else {
            // No interest filter — show all upcoming published (optional location)
            selectedTags = List.of();
            results      = adventureRepository.findUpcomingPublished(locationFilter, excludeHostId);
        }

        // Batch accepted counts — one query for all result IDs, no N+1.
        // Converts to Map<Long, Integer> (slotsLeft per adventure) for clean template comparisons.
        List<Long> resultIds = results.stream().map(Adventure::getId).toList();
        Map<Long, Integer> slotsLeftMap = Collections.emptyMap();
        if (!resultIds.isEmpty()) {
            // adventureId -> number of ACCEPTED participants
            Map<Long, Long> acceptedCounts = new HashMap<>();
            joinRequestRepository.countAcceptedByAdventureIds(resultIds)
                    .forEach(countRow -> acceptedCounts.put((Long) countRow[0], (Long) countRow[1]));
            slotsLeftMap = new HashMap<>();
            for (Adventure adventure : results) {
                // adventures with no accepted request are missing from the map -> default 0
                long acceptedCount = acceptedCounts.getOrDefault(adventure.getId(), 0L);
                slotsLeftMap.put(adventure.getId(),
                        (int) Math.max(0, adventure.getMaxParticipants() - acceptedCount));
            }
        }

        // Everything the explore.html template reads:
        model.addAttribute("adventures",     results);
        model.addAttribute("slotsLeftMap",   slotsLeftMap);
        model.addAttribute("selectedTags",   selectedTags);   // resolved Tag objects
        model.addAttribute("searchTags",     hasTags ? tags : List.of()); // slugs for chip pre-check
        model.addAttribute("searchLocation", hasLocation ? location.trim() : "");
        model.addAttribute("allTags",        tagRepository.findAllByOrderByNameAsc());
        model.addAttribute("userName",       principal != null ? principal.getName() : null);

        return "explore";
    }
}
