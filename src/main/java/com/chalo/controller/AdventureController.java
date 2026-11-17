package com.chalo.controller;

import com.chalo.dto.AdventureForm;
import com.chalo.model.Adventure;
import com.chalo.model.AdventurePhoto;
import com.chalo.model.AdventureStatus;
import com.chalo.model.JoinRequest;
import com.chalo.model.JoinRequestStatus;
import com.chalo.model.NotificationType;
import com.chalo.model.Tag;
import com.chalo.model.User;
import com.chalo.repository.AdventureRepository;
import com.chalo.repository.JoinRequestRepository;
import com.chalo.repository.TagRepository;
import com.chalo.repository.UserRepository;
import com.chalo.security.CustomUserDetails;
import com.chalo.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything a HOST does with an adventure: create it, review join requests, cancel it.
 * (The public detail page is in AdventureDetailsController.)
 *
 * URLs handled here:
 *   GET  /adventures/new              show the "Host an adventure" form
 *   POST /adventures                  save the new adventure
 *   POST /adventures/{id}/join        (older) send a join request without a notification
 *   GET  /adventures/{id}/requests    host sees the join requests
 *   POST /adventures/{id}/cancel      host cancels the adventure
 *
 * USEFUL ANNOTATIONS IN THIS FILE
 *   @Transactional        all database work in the method is one unit: either everything is
 *                         saved or (on an exception) nothing is.
 *   @PathVariable         takes a value from the URL: /adventures/{id}/cancel -> Long id.
 *   @RequestParam         takes a form field / query string value.
 *   RedirectAttributes    "flash attributes": data that survives ONE redirect (used for the
 *                         green success / red error banners).
 *   ResponseStatusException(HttpStatus.X) -> stops the request with that HTTP error page.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdventureController {

    // Each repository/service below is provided by Spring (constructor injection).
    private final TagRepository         tagRepository;
    private final AdventureRepository   adventureRepository;
    private final UserRepository        userRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final NotificationService   notificationService;

    // ── Host Adventure — show form ─────────────────────────────────────────────
    // GET /adventures/new is restricted to ROLE_USER in SecurityConfig.

    /** GET /adventures/new - shows the empty creation form. */
    @GetMapping("/adventures/new")
    public String showCreateForm(@AuthenticationPrincipal CustomUserDetails principal,
                                 Model model) {

        // When validation fails, createAdventure() returns to this page with the user's
        // input already in the model; in that case do not overwrite it with a blank form.
        if (!model.containsAttribute("form")) {
            AdventureForm form = new AdventureForm();
            form.getPhotoUrls().add("");   // render one empty photo URL row
            model.addAttribute("form", form);
        }
        addReferenceData(model, principal);
        return "adventures/new";
    }

    // ── Host Adventure — create ────────────────────────────────────────────────
    // POST /adventures falls under anyRequest().hasRole("USER"); CSRF is injected
    // by th:action. One repository.save() persists the adventure plus its cascaded
    // photos and tag join rows.

    /** POST /adventures - validates the form and saves a new PUBLISHED adventure. */
    @PostMapping("/adventures")
    @Transactional
    public String createAdventure(@Valid @ModelAttribute("form") AdventureForm form,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal CustomUserDetails principal,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        // Validation errors -> show the form again with the red messages
        if (bindingResult.hasErrors()) {
            addReferenceData(model, principal);
            return "adventures/new";
        }

        // Managed host reference without an extra SELECT (only the FK is needed)
        User host = userRepository.getReferenceById(principal.getId());

        // Build the Adventure from the validated form fields
        Adventure adventure = Adventure.builder()
                .host(host)
                .title(form.getTitle().trim())
                .description(form.getDescription().trim())
                .locationName(form.getLocation().trim())
                .adventureDate(form.getAdventureDate())
                .maxParticipants(form.getAvailableSlots())
                .status(AdventureStatus.PUBLISHED)   // goes live + discoverable
                .build();

        // Attach selected interests
        List<Tag> tags = tagRepository.findByIdIn(form.getTagIds());
        adventure.getTags().addAll(tags);

        // Attach photos (drop blank rows); first valid URL becomes the cover image
        List<String> photoUrls = form.getPhotoUrls().stream()
                .filter(photoUrl -> photoUrl != null && !photoUrl.isBlank())
                .map(String::trim)
                .toList();

        int displayPosition = 0;
        for (String photoUrl : photoUrls) {
            adventure.getPhotos().add(
                    AdventurePhoto.builder()
                            .adventure(adventure)
                            .url(photoUrl)
                            .uploadedBy(host)
                            .displayOrder(displayPosition++)
                            .build());
        }
        if (!photoUrls.isEmpty()) {
            adventure.setCoverImageUrl(photoUrls.get(0));
        }

        // Cascade saves the adventure + photos; the many-to-many saves the tag links.
        adventureRepository.save(adventure);
        redirectAttributes.addFlashAttribute("createSuccess", "Your adventure is live and ready for explorers to join!");
        return "redirect:/my-adventures";
    }

    // ── Submit Join Request ────────────────────────────────────────────────────
    // POST /adventures/{id}/join — requires ROLE_USER (SecurityConfig).
    // All guard checks run before persisting so the DB unique constraint is a
    // final safety net, not the primary validation.

    /**
     * POST /adventures/{id}/join - a traveller asks to join.
     * NOTE: the detail page's modal posts to /adventures/{id}/join-request instead
     * (AdventureDetailsController), which ALSO notifies the host. This older endpoint
     * does the same checks but sends no notification.
     */
    @PostMapping("/adventures/{id}/join")
    @Transactional
    public String submitJoin(@PathVariable Long id,
                              @RequestParam(required = false) String message,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {

        Adventure adventure = adventureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Guard 1: only PUBLISHED adventures accept requests
        if (adventure.getStatus() != AdventureStatus.PUBLISHED) {
            redirectAttributes.addFlashAttribute("joinError", "This adventure is not accepting requests.");
            return "redirect:/adventures/" + id;
        }

        // Guard 2: the host cannot join their own adventure
        // Lazy proxy FK read — no extra SELECT needed
        if (adventure.getHost().getId().equals(principal.getId())) {
            redirectAttributes.addFlashAttribute("joinError", "You cannot join your own adventure.");
            return "redirect:/adventures/" + id;
        }

        User requester = userRepository.getReferenceById(principal.getId());

        // Guard 3: only one request per person per adventure
        if (joinRequestRepository.existsByAdventureAndRequester(adventure, requester)) {
            redirectAttributes.addFlashAttribute("joinError", "You have already sent a request for this adventure.");
            return "redirect:/adventures/" + id;
        }

        // Guard 4: no free seats left
        long accepted = joinRequestRepository.countByAdventureAndStatus(adventure, JoinRequestStatus.ACCEPTED);
        if (accepted >= adventure.getMaxParticipants()) {
            redirectAttributes.addFlashAttribute("joinError", "This adventure is full.");
            return "redirect:/adventures/" + id;
        }

        // All checks passed -> store a PENDING request (status defaults to PENDING)
        joinRequestRepository.save(
                JoinRequest.builder()
                        .adventure(adventure)
                        .requester(requester)
                        .message(message != null && !message.isBlank() ? message.trim() : null)
                        .build());

        redirectAttributes.addFlashAttribute("joinSuccess", true);
        return "redirect:/adventures/" + id;
    }

    // ── View Join Requests (host only) ────────────────────────────────────────
    // GET /adventures/{id}/requests — requires ROLE_USER (SecurityConfig).
    // Returns 403 if the logged-in user is not the adventure's host.

    /** GET /adventures/{id}/requests - the host's page to accept or reject requests. */
    @GetMapping("/adventures/{id}/requests")
    @Transactional(readOnly = true)
    public String showRequests(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                Model model) {

        // findWithDetailById eagerly loads host via @EntityGraph
        Adventure adventure = adventureRepository.findWithDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Authorization: only the host of THIS adventure may see its requests
        if (!adventure.getHost().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<JoinRequest> requests = joinRequestRepository.findByAdventureWithRequester(adventure);
        long acceptedCount = joinRequestRepository.countByAdventureAndStatus(adventure, JoinRequestStatus.ACCEPTED);
        long pendingCount  = joinRequestRepository.countByAdventureAndStatus(adventure, JoinRequestStatus.PENDING);
        long slotsLeft     = Math.max(0, adventure.getMaxParticipants() - acceptedCount);

        model.addAttribute("adventure",     adventure);
        model.addAttribute("requests",      requests);
        model.addAttribute("acceptedCount", acceptedCount);
        model.addAttribute("pendingCount",  pendingCount);
        model.addAttribute("slotsLeft",     slotsLeft);
        model.addAttribute("userName",      principal.getName());

        return "adventures/requests";
    }

    // ── Cancel Adventure ──────────────────────────────────────────────────────
    // Only the host can cancel. Only PUBLISHED adventures can be cancelled —
    // CANCELLED is already terminal; DRAFT/COMPLETED are blocked.
    // All ACCEPTED participants receive an ADVENTURE_CANCELLED notification.
    // The adventure record and all join requests are preserved (soft cancel).

    /** POST /adventures/{id}/cancel - host cancels the adventure and notifies the crew. */
    @PostMapping("/adventures/{id}/cancel")
    @Transactional
    public String cancelAdventure(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails principal,
                                   RedirectAttributes redirectAttributes) {

        Adventure adventure = adventureRepository.findWithDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Only the host may cancel
        if (!adventure.getHost().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (adventure.getStatus() == AdventureStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("cancelError", "This adventure is already cancelled.");
            return "redirect:/adventures/" + id;
        }

        if (adventure.getStatus() != AdventureStatus.PUBLISHED) {
            redirectAttributes.addFlashAttribute("cancelError", "Only published adventures can be cancelled.");
            return "redirect:/adventures/" + id;
        }

        // Dirty-check: Hibernate emits the UPDATE on transaction commit
        adventure.setStatus(AdventureStatus.CANCELLED);

        // Notify every accepted participant in a single batch query
        List<JoinRequest> accepted = joinRequestRepository
                .findByAdventureWithRequester(adventure)
                .stream()
                .filter(joinRequest -> joinRequest.getStatus() == JoinRequestStatus.ACCEPTED)
                .toList();

        for (JoinRequest joinRequest : accepted) {
            notificationService.notifyUser(
                    joinRequest.getRequester().getId(),
                    "Adventure Cancelled",
                    "The host cancelled \"" + adventure.getTitle() + "\".",
                    NotificationType.ADVENTURE_CANCELLED,
                    "/adventures/" + id
            );
        }

        log.info("cancelAdventure: id={} host={} notified={} participants",
                id, principal.getId(), accepted.size());

        redirectAttributes.addFlashAttribute("cancelSuccess",
                "Adventure cancelled. " + accepted.size() + " participant(s) notified.");
        return "redirect:/my-adventures";
    }

    // ── Shared model data for the form view ────────────────────────────────────

    /** Adds the data every render of the "Host an adventure" page needs. */
    private void addReferenceData(Model model, CustomUserDetails principal) {
        model.addAttribute("allTags", tagRepository.findAllByOrderByNameAsc());
        model.addAttribute("today", LocalDate.now());   // date picker min
        model.addAttribute("userName", principal.getName());
    }
}
