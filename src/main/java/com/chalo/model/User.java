package com.chalo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A registered person. Maps to the MySQL table "users".
 *
 * ABOUT THE ANNOTATIONS ON THE CLASS
 *   @Entity            -> JPA/Hibernate: "this class is a database table".
 *   @Table(name=...)   -> the real table name (we avoid "user" because it is a reserved word).
 *   @Getter/@Setter    -> Lombok writes getName(), setName() ... for every field.
 *   @NoArgsConstructor -> empty constructor (JPA requires one).
 *   @AllArgsConstructor-> constructor with every field (needed by @Builder).
 *   @Builder           -> lets us write User.builder().name("A").email("a@b.c").build().
 *   @EqualsAndHashCode(of="id") -> two Users are "equal" if they have the same id.
 *   @ToString(exclude=...)      -> toString() skips "interests" to avoid loading lazy data.
 *
 * ABOUT VALIDATION ANNOTATIONS (@NotBlank, @Size ...)
 *   Bean Validation rules, checked by Hibernate right before saving to the database.
 *
 * Where used: AuthenticationService (create account), CustomUserDetailsService (login),
 * ProfileController (show/edit profile), and as the "host"/"requester"/"sender" of other entities.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"interests"})
public class User {

    /** Primary key. IDENTITY = MySQL AUTO_INCREMENT generates the number. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name shown across the site. Set at registration; editable on /profile/edit. */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /** Login identifier. Always saved lower-case and trimmed; must be unique. */
    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** The BCrypt HASH of the password (never the plain text). */
    @NotBlank
    @Column(nullable = false)
    private String password;

    /** Link to a profile picture. Shown on the adventure detail page's host card if present. */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Short "about me" text (max 500 chars via ProfileEditForm). Shown on profile + host card. */
    @Column(columnDefinition = "TEXT")
    private String bio;

    /** Phone number. Reserved for future use (no form fills it yet). */
    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    /** Age in years, collected at registration (1..120). */
    @Min(1)
    @Max(120)
    private Integer age;

    /** Gender (see Gender enum). Reserved for future use. Stored as text, e.g. "MALE". */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    /** Home city. Reserved for future use. */
    @Size(max = 100)
    @Column(length = 100)
    private String city;

    /**
     * true = this account is an administrator (gets ROLE_ADMIN, see CustomUserDetails).
     * Database column is "is_admin". Defaults to false for everyone who registers.
     */
    @Column(name = "is_admin", nullable = false)
    @Builder.Default
    private boolean admin = false;

    /** Filled automatically by Hibernate the first time the row is inserted. Never changes. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Filled automatically by Hibernate every time the row is updated. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * MANY-TO-MANY: a user can like many tags, a tag can be liked by many users.
     * Stored in the junction table "user_interests" (user_id, tag_id).
     * LAZY = tags are only loaded from the database when first accessed.
     * @BatchSize(20) = if many users' interests are needed, load 20 at a time (avoids N+1 queries).
     * Reserved for the future "Recommended For You" feature.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_interests",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Tag> interests = new HashSet<>();
}
