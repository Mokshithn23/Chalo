package com.chalo.service;

import com.chalo.model.Notification;
import com.chalo.model.NotificationType;
import com.chalo.model.User;
import com.chalo.repository.NotificationRepository;
import com.chalo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Creates and reads the in-app notifications (the bell icon).
 *
 * Called from:
 *   - AdventureDetailsController.submitJoinRequest  (notify host)
 *   - JoinRequestController.accept / reject         (notify traveller)
 *   - AdventureController.cancelAdventure           (notify participants)
 *   - NotificationController / NotificationCountAdvice (read + count)
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    // Database access to "notifications" and "users" tables
    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    // ── Create ────────────────────────────────────────────────────────────────
    // Accepts userId (not a User entity) to avoid forcing callers to load the
    // full entity — getReferenceById gives a proxy sufficient for the FK column.

    /**
     * Saves a new unread notification for a user.
     *
     * @param userId  who receives it
     * @param title   short headline
     * @param message the sentence shown under the headline
     * @param type    category (decides the icon)
     * @param link    page to open when clicked (e.g. "/adventures/5"); may be null
     */
    @Transactional
    public void notifyUser(Long userId, String title, String message,
                           NotificationType type, String link) {
        // Placeholder with only the id (no SELECT needed to fill the foreign key)
        User userReference = userRepository.getReferenceById(userId);
        notificationRepository.save(
                Notification.builder()
                        .user(userReference)
                        .title(title)
                        .message(message)
                        .type(type)
                        .link(link)
                        .build()
        );
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All notifications of a user, newest first (for the /notifications page). */
    @Transactional(readOnly = true)
    public List<Notification> getAllForUser(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Number of unread notifications (the red badge on the bell, shown on every page). */
    @Transactional(readOnly = true)
    public long getUnreadCountForUser(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Marks every unread notification of the user as read ("Mark all as read" button). */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    // Marks the notification as read and returns its link (for click-to-navigate).
    // Combines both operations in one transaction to avoid a second round-trip.
    /**
     * Marks ONE notification as read and returns where it points to.
     * The query also checks the notification belongs to this user, so nobody can read
     * somebody else's notification by guessing ids (404 otherwise).
     *
     * @param notificationId id of the notification
     * @param userId         id of the logged-in user (the owner)
     * @return the notification's link (may be null)
     */
    @Transactional
    public String markAsReadAndGetLink(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Changing a loaded entity is enough; Hibernate writes the UPDATE when the transaction ends.
        notification.setRead(true);
        return notification.getLink();
    }

    // ── Time-ago helper ───────────────────────────────────────────────────────
    // Static so the NotificationController can reference it without circular dep.
    // Uses JVM local time — consistent with @CreationTimestamp which also uses
    // the JVM clock.

    /**
     * Turns a timestamp into friendly text: "just now", "5m ago", "3h ago", "2d ago", "1w ago", "2mo ago".
     *
     * @param dateTime the moment something happened
     * @return a short "time ago" string
     */
    public static String timeAgo(LocalDateTime dateTime) {
        long secondsAgo = ChronoUnit.SECONDS.between(dateTime, LocalDateTime.now());
        if (secondsAgo < 60)  return "just now";
        long minutesAgo = secondsAgo / 60;
        if (minutesAgo < 60)  return minutesAgo + "m ago";
        long hoursAgo = minutesAgo / 60;
        if (hoursAgo < 24) return hoursAgo + "h ago";
        long daysAgo = hoursAgo / 24;
        if (daysAgo < 7)   return daysAgo + "d ago";
        long weeksAgo = daysAgo / 7;
        if (weeksAgo < 5)  return weeksAgo + "w ago";
        return ChronoUnit.MONTHS.between(dateTime, LocalDateTime.now()) + "mo ago";
    }
}
