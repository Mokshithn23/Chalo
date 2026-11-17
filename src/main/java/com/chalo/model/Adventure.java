package com.chalo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A trip/experience that a HOST publishes and other users ask to join.
 * Maps to table "adventures". This is the central entity of the application.
 *
 * Relationships (see the ER diagram in README.md):
 *   User (host)  1 ──── *  Adventure
 *   Adventure    * ──── *  Tag              (junction table adventure_tags)
 *   Adventure    1 ──── *  AdventurePhoto
 *   Adventure    1 ──── *  JoinRequest
 *   Adventure    1 ──── 0..1 Chat
 *
 * (Lombok annotations are explained in User.java and in README.md.)
 */
@Entity
@Table(name = "adventures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
// Exclude relationship fields from toString() so printing an Adventure never triggers
// extra database queries or infinite loops (Adventure -> photos -> Adventure ...).
@ToString(exclude = {"host", "tags", "photos", "joinRequests", "chat"})
public class Adventure {

    /** Primary key (auto-increment). Appears in URLs, e.g. /adventures/5. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who created (hosts) this adventure. Foreign key column host_id.
     * MANY adventures -> ONE host. LAZY = the User row is loaded only when needed.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    /** Short headline (max 150 chars). */
    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String title;

    /** Long free text describing the plan. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** The day the adventure happens (date only, no time). Used to hide past adventures. */
    @NotNull
    @Column(name = "adventure_date", nullable = false)
    private LocalDate adventureDate;

    /** Where it happens, as typed by the host (e.g. "Gokarna, Karnataka"). Searched by /explore. */
    @Size(max = 255)
    @Column(name = "location_name", length = 255)
    private String locationName;

    /** GPS latitude. Reserved for a future map feature; currently never filled. */
    @Column(name = "location_lat", precision = 9, scale = 6)
    private BigDecimal locationLat;

    /** GPS longitude. Reserved for a future map feature; currently never filled. */
    @Column(name = "location_lng", precision = 9, scale = 6)
    private BigDecimal locationLng;

    /** Total seats. Free seats = maxParticipants - number of ACCEPTED join requests. */
    @NotNull
    @Min(1)
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    /** Image shown on cards. Automatically the first photo URL the host enters. */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /** Current life-cycle stage (see AdventureStatus). New adventures start as PUBLISHED. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AdventureStatus status = AdventureStatus.DRAFT;

    /** Set once by Hibernate when the row is first saved. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Refreshed by Hibernate on every update. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Interests this adventure belongs to (MANY-TO-MANY with Tag).
     * Stored in junction table "adventure_tags" (adventure_id, tag_id).
     * Used by /explore to match adventures to the user's chosen interest chips.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "adventure_tags",
            joinColumns = @JoinColumn(name = "adventure_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    /**
     * Pictures of the adventure (ONE-TO-MANY). "mappedBy" means the foreign key lives in
     * AdventurePhoto.adventure. cascade=ALL: saving/deleting an Adventure also saves/deletes
     * its photos. orphanRemoval=true: removing a photo from this list deletes the DB row.
     * @OrderBy keeps them sorted by displayOrder.
     */
    @OneToMany(mappedBy = "adventure", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<AdventurePhoto> photos = new ArrayList<>();

    /** All requests people made to join this adventure (ONE-TO-MANY, same rules as photos). */
    @OneToMany(mappedBy = "adventure", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<JoinRequest> joinRequests = new ArrayList<>();

    /** The group chat of this adventure (ONE-TO-ONE; null until somebody opens the chat). */
    @OneToOne(mappedBy = "adventure", cascade = CascadeType.ALL, orphanRemoval = true)
    private Chat chat;
}
