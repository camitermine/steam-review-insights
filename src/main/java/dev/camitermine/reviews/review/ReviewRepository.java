package dev.camitermine.reviews.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data genera la implementación a partir del nombre de cada método.
 */
public interface ReviewRepository extends JpaRepository<Review, String> {

    Page<Review> findByAppId(long appId, Pageable pageable);

    Page<Review> findByAppIdAndVotedUp(long appId, boolean votedUp, Pageable pageable);

    long countByAppId(long appId);

    long countByAppIdAndVotedUp(long appId, boolean votedUp);
}
