package com.chalo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One picture (stored as an image URL, not as a file) belonging to an Adventure.
 * Maps to table "adventure_photos".
 *
 * Created in AdventureController.createAdventure() from the photo URL boxes of the
 * "Host an Adventure" form; displayed in the gallery of adventure-details.html.
 */
@Entity
@Table(name = "adventure_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"adventure", "uploadedBy"})
public class AdventurePhoto {

    /** Primary key (auto-increment). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The adventure this photo belongs to (foreign key adventure_id). */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adventure_id", nullable = false)
    private Adventure adventure;

    /** Web address of the image, e.g. https://images.example.com/photo.jpg */
    @NotBlank
    @Column(nullable = false, length = 500)
    private String url;

    /** Optional text under the picture. */
    @Size(max = 255)
    @Column(length = 255)
    private String caption;

    /** Sort position in the gallery: 0 = first (also used as the cover image). */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /** The user who added the photo (foreign key uploaded_by) - currently always the host. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    /** Set automatically when the row is inserted. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
