package com.chalo.repository;

import com.chalo.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Database access for "notifications".
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ── Fetch all for a user, newest first ────────────────────────────────────
    /** Every notification of a user, newest first. */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.user.id = :userId
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // ── Unread count — drives the header badge ────────────────────────────────
    /** Number of unread notifications of a user (the bell badge). */
    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.user.id = :userId AND n.read = false
            """)
    long countUnreadByUserId(@Param("userId") Long userId);

    // ── Single notification with ownership check (no lazy user load needed) ──
    /** One notification, but only if it belongs to that user (ownership check). */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.id = :id AND n.user.id = :userId
            """)
    Optional<Notification> findByIdAndUserId(@Param("id") Long id,
                                             @Param("userId") Long userId);

    // ── Bulk mark-all-read ────────────────────────────────────────────────────
    /**
     * Marks all unread notifications of a user as read with ONE UPDATE statement.
     * @Modifying tells Spring Data this query changes data (UPDATE) rather than reads it.
     */
    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.read = true
            WHERE n.user.id = :userId AND n.read = false
            """)
    void markAllReadByUserId(@Param("userId") Long userId);
}
