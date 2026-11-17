package com.chalo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * "I would like to join your adventure" - a request from a traveller to a host.
 * Maps to table "join_requests".
 *
 * LIFE: created as PENDING (AdventureDetailsController.submitJoinRequest)
 *       -> host presses Accept/Reject (JoinRequestController) -> ACCEPTED or REJECTED.
 *
 * The @UniqueConstraint below means the database itself refuses a second request
 * from the same user for the same adventure (safety net on top of the Java checks).
 */
@Entity
@Table(
        name = "join_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_adventure_requester",
                columnNames = {"adventure_id", "requester_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"adventure", "requester"})
public class JoinRequest {

    /** Primary key (auto-increment). Used in URLs: POST /requests/{id}/accept. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which adventure the person wants to join (foreign key adventure_id). */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adventure_id", nullable = false)
    private Adventure adventure;

    /** Who is asking (foreign key requester_id -> users.id). */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** Optional note the traveller writes to the host ("I have trekked before ..."). */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** PENDING / ACCEPTED / REJECTED. New requests are PENDING. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    /** When the request was sent (automatic). */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** When the request last changed, e.g. when it was accepted (automatic). */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
