package com.chalo.repository;

import com.chalo.model.Adventure;
import com.chalo.model.AdventurePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Database access for "adventure_photos".
 * NOTE: not used anywhere yet - photos are saved through Adventure's cascade
 * (adventure.getPhotos().add(...) + adventureRepository.save(adventure)).
 */
public interface AdventurePhotoRepository extends JpaRepository<AdventurePhoto, Long> {

    /** Photos of one adventure in gallery order. */
    List<AdventurePhoto> findByAdventureOrderByDisplayOrderAsc(Adventure adventure);

    // Shift display_order values when re-ordering — caller updates each photo's
    // displayOrder field then calls saveAll().
    /** How many photos an adventure has. */
    long countByAdventure(Adventure adventure);
}
