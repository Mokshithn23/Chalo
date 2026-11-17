package com.chalo.model;

/**
 * The life-cycle stage of an Adventure. Stored in the database as TEXT
 * (because the Adventure entity uses @Enumerated(EnumType.STRING)).
 *
 * Typical flow:  PUBLISHED  ->  COMPLETED   (or)   PUBLISHED  ->  CANCELLED
 *
 * Where used:
 *   - AdventureController.createAdventure()  sets PUBLISHED when a host creates an adventure.
 *   - AdventureController.cancelAdventure()  moves PUBLISHED -> CANCELLED.
 *   - AdventureRepository queries only return PUBLISHED adventures to the public.
 *   - Templates show the status as a coloured pill (my-adventures.html, my-crews.html).
 *
 * NOTE: nothing in the code sets DRAFT or COMPLETED yet; they are reserved for future
 *       features (saving drafts, marking finished trips).
 */
public enum AdventureStatus {
    /** Saved but not visible to the public (future feature). */
    DRAFT,
    /** Live: visible on /explore and open for join requests. */
    PUBLISHED,
    /** The trip has happened (future feature). */
    COMPLETED,
    /** The host cancelled it. Still viewable via notification links, but no new joins. */
    CANCELLED
}
