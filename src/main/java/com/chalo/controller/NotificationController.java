package com.chalo.controller;

import com.chalo.model.Notification;
import com.chalo.security.CustomUserDetails;
import com.chalo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The notification centre (bell icon).
 *
 * URLs handled here (login required):
 *   GET  /notifications            list unread + read notifications
 *   GET  /notifications/{id}/go    mark one as read and open its link
 *   POST /notifications/{id}/read  mark one as read, stay on the list
 *   POST /notifications/read-all   mark everything as read
 */
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── Notifications page ────────────────────────────────────────────────────

    /** Loads the user's notifications and splits them into "unread" and "read" lists. */
    @GetMapping("/notifications")
    @Transactional(readOnly = true)
    public String showNotifications(@AuthenticationPrincipal CustomUserDetails principal,
                                    Model model) {

        List<Notification> allNotifications = notificationService.getAllForUser(principal.getId());

        List<Notification> unreadNotifications = allNotifications.stream().filter(notification -> !notification.isRead()).toList();
        List<Notification> readNotifications   = allNotifications.stream().filter(Notification::isRead).toList();

        // notificationId -> "5m ago" text, so the template can show a friendly time
        Map<Long, String> timeAgo = allNotifications.stream()
                .collect(Collectors.toMap(
                        Notification::getId,
                        notification -> NotificationService.timeAgo(notification.getCreatedAt())
                ));

        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("readNotifications",   readNotifications);
        model.addAttribute("timeAgo",             timeAgo);
        model.addAttribute("userName",            principal.getName());
        return "notifications";
    }

    // ── Click-to-navigate: marks as read then redirects to the notification's
    //    link. Returning to /notifications if the notification has no link.
    //    GET is appropriate here — this is an idempotent "open notification" action.

    /** Marks the notification as read, then redirects to the page it points to. */
    @GetMapping("/notifications/{id}/go")
    @Transactional
    public String goToNotification(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails principal) {
        String link = notificationService.markAsReadAndGetLink(id, principal.getId());
        return (link != null && !link.isBlank()) ? "redirect:" + link : "redirect:/notifications";
    }

    // ── Mark a single notification as read (without navigating away) ──────────

    /** Marks one notification as read and returns to the list. */
    @PostMapping("/notifications/{id}/read")
    @Transactional
    public String markRead(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAsReadAndGetLink(id, principal.getId());
        return "redirect:/notifications";
    }

    // ── Mark all as read ──────────────────────────────────────────────────────

    /** "Mark all as read" button. */
    @PostMapping("/notifications/read-all")
    @Transactional
    public String markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAllAsRead(principal.getId());
        return "redirect:/notifications";
    }
}
