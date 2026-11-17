package com.chalo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * An "interest" label such as Trekking, Camping or Beach. Maps to table "tags".
 *
 * Tags are the heart of the "experience-first" idea: users do not browse categories,
 * they pick interests, and adventures are tagged with the same interests.
 *
 * Where used:
 *   - TagDataLoader        seeds the default tags at every startup.
 *   - DashboardController  shows them as search chips.
 *   - AdventureController  lets the host attach tags to a new adventure.
 *   - ExploreController    filters adventures by tag slug.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class Tag {

    /** Primary key (auto-increment). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human readable name, e.g. "Food Exploring". Unique. */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** URL-safe version of the name, e.g. "food-exploring". Used in /explore?tags=food-exploring. Unique. */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    /** Bootstrap-Icons CSS classes, e.g. "bi bi-fire". Rendered as <i class="bi bi-fire"> in templates. */
    @Size(max = 100)
    @Column(length = 100)
    private String icon;
}
