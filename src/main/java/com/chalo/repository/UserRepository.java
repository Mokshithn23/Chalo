package com.chalo.repository;

import com.chalo.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Database access for the "users" table.
 *
 * WHAT IS A REPOSITORY?
 *   An interface that extends JpaRepository<Entity, IdType>. You write NO implementation:
 *   Spring Data JPA generates it at startup. You get save(), findById(), findAll(),
 *   deleteById(), count() ... for free, plus any method whose NAME describes a query
 *   (e.g. findByEmail -> SELECT * FROM users WHERE email = ?).
 *
 * Optional<User> = "maybe a user, maybe nothing" (avoids returning null).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Finds a user by email. Used at login (CustomUserDetailsService). */
    Optional<User> findByEmail(String email);

    /** true if an account with this email exists. Used at registration (AuthenticationService). */
    boolean existsByEmail(String email);

    // Loads interests in the same query — used before building recommendations
    /**
     * Loads a user TOGETHER with their interest tags in one query (@EntityGraph).
     * Currently unused; prepared for the future "Recommended For You" feature.
     */
    @EntityGraph(attributePaths = "interests")
    Optional<User> findWithInterestsById(Long id);
}
