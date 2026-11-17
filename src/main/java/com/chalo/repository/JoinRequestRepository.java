package com.chalo.repository;

import com.chalo.model.Adventure;
import com.chalo.model.JoinRequest;
import com.chalo.model.JoinRequestStatus;
import com.chalo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Database access for "join_requests".
 *
 * Vocabulary used in the queries below (JPQL):
 *   jr            = alias for a JoinRequest row
 *   a             = alias for its Adventure
 *   JOIN FETCH    = "load the related row in the SAME query". Without it, showing a list of
 *                   N requests would run 1 + N queries (the famous "N+1 problem").
 *   GROUP BY/COUNT= SQL grouping: one result row per adventure with a counter.
 */
public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    // ── Host: manage requests for their adventure ─────────────────────────────
    // Loads requester eagerly to avoid N+1 on the requests management page.
    /** All requests of one adventure (newest first) with the requesting users already loaded. */
    @Query("""
            SELECT jr FROM JoinRequest jr
            JOIN FETCH jr.requester
            WHERE jr.adventure = :adventure
            ORDER BY jr.createdAt DESC
            """)
    List<JoinRequest> findByAdventureWithRequester(@Param("adventure") Adventure adventure);

    // ── Requester: duplicate-request guard ────────────────────────────────────
    /** The request (if any) that this user already made for this adventure. */
    Optional<JoinRequest> findByAdventureAndRequester(Adventure adventure, User requester);

    /** true if this user already asked to join this adventure (duplicate guard). */
    boolean existsByAdventureAndRequester(Adventure adventure, User requester);

    // ── Dashboard: my outgoing requests ───────────────────────────────────────
    // Loads adventure (title + date) eagerly for display.
    /** All requests sent by a user (any status) with adventure loaded. Not used yet. */
    @Query("""
            SELECT jr FROM JoinRequest jr
            JOIN FETCH jr.adventure
            WHERE jr.requester = :requester
            ORDER BY jr.createdAt DESC
            """)
    List<JoinRequest> findByRequesterWithAdventure(@Param("requester") User requester);

    // ── Status filter ─────────────────────────────────────────────────────────
    /** Requests of an adventure having a given status. Not used yet. */
    List<JoinRequest> findByAdventureAndStatus(Adventure adventure, JoinRequestStatus status);

    /**
     * How many requests of an adventure have a given status.
     * With ACCEPTED it gives "seats taken"; slots left = maxParticipants - this number.
     */
    long countByAdventureAndStatus(Adventure adventure, JoinRequestStatus status);

    // ── My Adventures: pending count for a batch of adventures (one query) ────
    // Returns [adventure_id, count] rows — adventures with zero pending requests
    // are absent. Caller converts to Map<Long,Long> and uses getOrDefault(id, 0).
    /**
     * For many adventures at once: how many PENDING requests each has.
     * Each returned row is an Object[]: [0] = adventure id, [1] = count.
     * Used for the red "pending requests" badge in My Adventures.
     */
    @Query("""
            SELECT jr.adventure.id, COUNT(jr)
            FROM JoinRequest jr
            WHERE jr.adventure.id IN :adventureIds
              AND jr.status = com.chalo.model.JoinRequestStatus.PENDING
            GROUP BY jr.adventure.id
            """)
    List<Object[]> countPendingByAdventureIds(@Param("adventureIds") List<Long> adventureIds);

    // ── Requester: "Adventures I've Joined" — ACCEPTED only ──────────────────
    // JOIN FETCH adventure + host in one query. Tags on adventure batch-load via
    // @BatchSize(20) during template render — no extra round-trips per card.
    /** A user's ACCEPTED requests ("Adventures I've joined"), soonest adventure first. */
    @Query("""
            SELECT jr FROM JoinRequest jr
            JOIN FETCH jr.adventure a
            JOIN FETCH a.host
            WHERE jr.requester = :requester
              AND jr.status = com.chalo.model.JoinRequestStatus.ACCEPTED
            ORDER BY a.adventureDate ASC
            """)
    List<JoinRequest> findAcceptedByRequesterWithAdventureAndHost(@Param("requester") User requester);

    // ── Requester: "Pending Requests" — outgoing PENDING only ────────────────
    // Same JOIN FETCH pattern as above — host name needed for card display.
    /** A user's still-PENDING requests ("Waiting for the host"), newest first. */
    @Query("""
            SELECT jr FROM JoinRequest jr
            JOIN FETCH jr.adventure a
            JOIN FETCH a.host
            WHERE jr.requester = :requester
              AND jr.status = com.chalo.model.JoinRequestStatus.PENDING
            ORDER BY jr.createdAt DESC
            """)
    List<JoinRequest> findPendingByRequesterWithAdventureAndHost(@Param("requester") User requester);

    // ── Batch accepted count per adventure (slots-left for joined cards) ──────
    // Returns [adventure_id, count] rows — absent entries mean zero accepted.
    /**
     * For many adventures at once: how many ACCEPTED requests (= seats taken) each has.
     * Rows are Object[]: [0] = adventure id, [1] = count. Adventures with 0 are missing.
     * Used by /explore, My Adventures and My Crews to show "X spots left".
     */
    @Query("""
            SELECT jr.adventure.id, COUNT(jr)
            FROM JoinRequest jr
            WHERE jr.adventure.id IN :adventureIds
              AND jr.status = com.chalo.model.JoinRequestStatus.ACCEPTED
            GROUP BY jr.adventure.id
            """)
    List<Object[]> countAcceptedByAdventureIds(@Param("adventureIds") List<Long> adventureIds);

    // ── Host statistics: total accepted participants across all hosted adventures
    /** Total accepted participants across all adventures of one host. Not used yet. */
    @Query("""
            SELECT COUNT(jr) FROM JoinRequest jr
            WHERE jr.adventure.host.id = :hostId
              AND jr.status = com.chalo.model.JoinRequestStatus.ACCEPTED
            """)
    long countAcceptedByHostId(@Param("hostId") Long hostId);

    // ── Accept / Reject: one query, no lazy-load surprises ───────────────────
    /** One request with its adventure and the adventure's host loaded (for accept/reject checks). */
    @Query("""
            SELECT jr FROM JoinRequest jr
            JOIN FETCH jr.adventure a
            JOIN FETCH a.host
            WHERE jr.id = :id
            """)
    Optional<JoinRequest> findByIdWithAdventureAndHost(@Param("id") Long id);

    // ── Crew Chat authorization: accepted participant check ───────────────────
    // Used by CrewChatService.canAccess() — host check is a simple ID equality,
    // this covers the participant side without touching ChatParticipant.
    /** true if the user has a request with this status for this adventure (by ids, no entity loading). */
    boolean existsByAdventureIdAndRequesterIdAndStatus(Long adventureId,
                                                       Long requesterId,
                                                       JoinRequestStatus status);

    // ── Admin ─────────────────────────────────────────────────────────────────
    /** Reserved for the future admin panel. */
    List<JoinRequest> findByStatusOrderByCreatedAtDesc(JoinRequestStatus status);
}
