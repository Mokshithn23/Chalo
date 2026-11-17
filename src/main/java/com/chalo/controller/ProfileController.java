package com.chalo.controller;

import com.chalo.dto.ProfileEditForm;
import com.chalo.model.Adventure;
import com.chalo.model.JoinRequest;
import com.chalo.model.User;
import com.chalo.repository.AdventureRepository;
import com.chalo.repository.JoinRequestRepository;
import com.chalo.repository.UserRepository;
import com.chalo.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * The user's own profile.
 *
 * URLs handled here (login required):
 *   GET  /profile        profile.html      (stats + preview of hosted / joined adventures)
 *   GET  /profile/edit   profile-edit.html (form)
 *   POST /profile/edit   save name + bio
 */
@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository        userRepository;
    private final AdventureRepository   adventureRepository;
    private final JoinRequestRepository joinRequestRepository;

    // ── Profile ──────────────────────────────────────────────────────────────
    // Loads the full User entity (not getReferenceById) because the template
    // needs scalar fields — name, email, createdAt — that are not on the
    // security principal.
    //
    // Query strategy (all existing methods, no new repository queries):
    //   hosted  → findByHostOrderByAdventureDateDesc — ordered DESC by date,
    //             first 3 become the preview; .size() gives the total count.
    //   joined  → findAcceptedByRequesterWithAdventureAndHost — JOIN FETCHes
    //             adventure + host in one query; first 3 become preview.
    //   pending → findPendingByRequesterWithAdventureAndHost — only .size()
    //             is used; the list itself is not passed to the template.

    /** Shows the profile page with counters and 3-item previews. */
    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public String showProfile(@AuthenticationPrincipal CustomUserDetails principal,
                              Model model) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Adventure>   hostedAdventures = adventureRepository.findByHostOrderByAdventureDateDesc(user);
        List<JoinRequest> joinedRequests   = joinRequestRepository.findAcceptedByRequesterWithAdventureAndHost(user);
        List<JoinRequest> pendingRequests  = joinRequestRepository.findPendingByRequesterWithAdventureAndHost(user);

        model.addAttribute("user",          user);
        model.addAttribute("userName",      user.getName());
        model.addAttribute("hostedCount",   hostedAdventures.size());
        model.addAttribute("joinedCount",   joinedRequests.size());
        model.addAttribute("pendingCount",  pendingRequests.size());
        // subList(0, min(3, size)) = "the first three (or fewer)"
        model.addAttribute("hostedPreview", hostedAdventures.subList(0, Math.min(3, hostedAdventures.size())));
        model.addAttribute("joinedPreview", joinedRequests.subList(0, Math.min(3, joinedRequests.size())));

        return "profile";
    }

    // ── Edit Profile — show form ──────────────────────────────────────────────

    /** GET /profile/edit - the form, pre-filled with the current name and bio. */
    @GetMapping("/profile/edit")
    @Transactional(readOnly = true)
    public String showEditForm(@AuthenticationPrincipal CustomUserDetails principal,
                               Model model) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Keep user's typed input if we came back here after a validation error
        if (!model.containsAttribute("form")) {
            ProfileEditForm form = new ProfileEditForm();
            form.setName(user.getName());
            form.setBio(user.getBio());
            model.addAttribute("form", form);
        }
        model.addAttribute("userName", user.getName());
        return "profile-edit";
    }

    // ── Edit Profile — save ───────────────────────────────────────────────────

    /** POST /profile/edit - validates and saves the new name and bio. */
    @PostMapping("/profile/edit")
    @Transactional
    public String saveProfile(@Valid @ModelAttribute("form") ProfileEditForm form,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("userName", principal.getName());
            return "profile-edit";
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Changing the loaded entity is enough; Hibernate saves it when the transaction ends.
        user.setName(form.getName().trim());
        user.setBio(form.getBio() != null && !form.getBio().isBlank() ? form.getBio().trim() : null);

        // NOTE: the name in the logged-in session (CustomUserDetails) is not refreshed,
        // so the header may show the old name until the next login.
        redirectAttributes.addFlashAttribute("profileSuccess", "Profile updated successfully.");
        return "redirect:/profile";
    }
}
