package com.chalo.repository;

import com.chalo.model.Adventure;
import com.chalo.model.AdventureStatus;
import com.chalo.model.Tag;
import com.chalo.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Database access for the "adventures" table - the most query-heavy repository.
 *
 * HOW TO READ THE @Query BLOCKS BELOW
 *   They are written in JPQL/HQL: SQL-like, but it talks about Java classes and fields
 *   (Adventure, a.tags, a.host.id) instead of tables and columns.
 *     "a" and "t" are short aliases (a = Adventure, t = Tag).
 *     ":location" is a named parameter filled from the @Param("location") argument.
 *     JOIN a.tags t      -> join the adventure with its tags (through adventure_tags).
 *     CURRENT_DATE       -> today, evaluated by the database.
 *   Do not change the query text without testing: a typo here stops the whole app from starting,
 *   because Spring validates every query at startup.
 */
public interface AdventureRepository extends JpaRepository<Adventure, Long> {

    // ── Detail page ──────────────────────────────────────────────────────────
    // Fetches tags, photos, and host in one query to avoid lazy-load round trips
    // on the detail page template.
    /**
     * One adventure with its tags, photos and host loaded in a single query (@EntityGraph).
     * The words "WithDetail" in the name are ignored by Spring Data; it is just "find ... ById".
     * Used by: AdventureDetailsController.showDetail, AdventureController (requests, cancel),
     * CrewChatController.
     */
    @EntityGraph(attributePaths = {"tags", "photos", "host"})
    Optional<Adventure> findWithDetailById(Long id);

    // ── Explore / Search — tag-filtered ──────────────────────────────────────
    // Returns DISTINCT published upcoming adventures whose tag set intersects
    // the selected tag IDs. Location is an optional case-insensitive substring
    // match. Excludes adventures hosted by excludeHostId (pass -1 for guests).
    // @BatchSize(size=20) on Adventure.tags batches the lazy tag loads.
    /**
     * Search used by /explore when at least one interest chip is selected.
     * Rules: status = PUBLISHED, date today or later, has at least one of the tags,
     * location contains the text (if given), and is not hosted by the viewer. Soonest first.
     *
     * @param tagIds        ids of the selected tags
     * @param location      text to look for inside the location name, or null for "any"
     * @param excludeHostId viewer's user id (their own adventures are hidden); -1 for guests
     */
    @Query("""
            SELECT DISTINCT a FROM Adventure a
            JOIN a.tags t
            WHERE t.id IN :tagIds
              AND (:location IS NULL
                   OR LOWER(a.locationName) LIKE LOWER(CONCAT('%', :location, '%')))
              AND a.status = com.chalo.model.AdventureStatus.PUBLISHED
              AND a.adventureDate >= CURRENT_DATE
              AND a.host.id <> :excludeHostId
            ORDER BY a.adventureDate ASC
            """)
    List<Adventure> search(@Param("tagIds") List<Long> tagIds,
                           @Param("location") String location,
                           @Param("excludeHostId") Long excludeHostId);

    // ── Explore / Search — no tags (show all or location-only) ───────────────
    // Used when the user submits the search form with no interest chips checked.
    // Location filter and host exclusion work the same way as search().
    /**
     * Search used by /explore when NO interest chip is selected: all upcoming
     * published adventures, optionally filtered by location text.
     */
    @Query("""
            SELECT a FROM Adventure a
            WHERE (:location IS NULL
                   OR LOWER(a.locationName) LIKE LOWER(CONCAT('%', :location, '%')))
              AND a.status = com.chalo.model.AdventureStatus.PUBLISHED
              AND a.adventureDate >= CURRENT_DATE
              AND a.host.id <> :excludeHostId
            ORDER BY a.adventureDate ASC
            """)
    List<Adventure> findUpcomingPublished(@Param("location") String location,
                                          @Param("excludeHostId") Long excludeHostId);

    // ── Recommended For You (Dashboard) ──────────────────────────────────────
    // Returns published adventures ordered by the number of tags that overlap
    // with the current user's interests. Excludes adventures the user hosts.
    // Caller must ensure interests is non-empty before calling (empty IN clause
    // returns no results by design).
    /**
     * "Recommended for you": adventures sorted by how many tags match the user's interests.
     * NOT used yet - the dashboard currently shows placeholder cards (see DashboardController).
     */
    @Query("""
            SELECT a FROM Adventure a
            JOIN a.tags t
            WHERE t IN :interests
              AND a.status = com.chalo.model.AdventureStatus.PUBLISHED
              AND a.adventureDate >= CURRENT_DATE
              AND a.host.id <> :userId
            GROUP BY a
            ORDER BY COUNT(t) DESC
            """)
    List<Adventure> findRecommended(@Param("interests") Set<Tag> interests,
                                    @Param("userId") Long userId,
                                    Pageable pageable);

    // ── Dashboard — my hosted adventures ─────────────────────────────────────
    /** Every adventure hosted by a user, latest date first (My Adventures, My Crews, Profile). */
    List<Adventure> findByHostOrderByAdventureDateDesc(User host);

    // ── Host profile card on detail page (3 most recent) ─────────────────────
    /**
     * A host's PUBLISHED or COMPLETED adventures, newest date first.
     * Used for the "More from this host" strip on the detail page (Pageable limits the count).
     */
    @Query("""
            SELECT a FROM Adventure a
            WHERE a.host.id = :hostId
              AND a.status IN (
                  com.chalo.model.AdventureStatus.PUBLISHED,
                  com.chalo.model.AdventureStatus.COMPLETED)
            ORDER BY a.adventureDate DESC
            """)
    List<Adventure> findPublishedByHostId(@Param("hostId") Long hostId, Pageable pageable);

    // ── Host statistics ───────────────────────────────────────────────────────
    /** How many adventures this host has created (used for host badges). */
    long countByHost(User host);

    /** How many of this host's adventures have the given status (e.g. COMPLETED). */
    long countByHostAndStatus(User host, AdventureStatus status);

    // ── Admin ─────────────────────────────────────────────────────────────────
    /** Reserved for the future admin panel: adventures of one status, newest first. */
    List<Adventure> findByStatusOrderByCreatedAtDesc(AdventureStatus status);
}
