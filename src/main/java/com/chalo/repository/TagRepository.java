package com.chalo.repository;

import com.chalo.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Database access for the "tags" table (interest labels).
 * Method names are turned into SQL by Spring Data (see UserRepository for the idea).
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** All tags A-Z by name. Used for the interest chips on dashboard, explore and "host" form. */
    List<Tag> findAllByOrderByNameAsc();

    /** One tag by its slug. Used by TagDataLoader to decide insert-or-update. */
    Optional<Tag> findBySlug(String slug);

    // Bulk-load tags selected by the user on the search/profile form
    /** Several tags at once by id (the ticked boxes of the host form). */
    List<Tag> findByIdIn(Collection<Long> ids);

    // Resolve multiple slugs at once — used by ExploreController to convert
    // ?tags= slug params from the dashboard search form into Tag entities.
    /** Several tags at once by slug (?tags=trekking&tags=camping on /explore). */
    List<Tag> findBySlugIn(Collection<String> slugs);
}
