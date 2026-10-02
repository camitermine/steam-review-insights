package dev.camitermine.reviews.review;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data genera la implementación a partir del nombre de cada método
 * (o de la consulta JPQL en @Query).
 */
public interface ReviewRepository extends JpaRepository<Review, String> {

    /** Los filtros en null no se aplican. */
    @Query("""
            select r from Review r
            where r.appId = :appId
              and (:votedUp is null or r.votedUp = :votedUp)
              and (:category is null or r.category = :category)
            """)
    Page<Review> search(long appId, Boolean votedUp, ReviewCategory category, Pageable pageable);

    List<Review> findByAppIdAndCategoryIsNull(long appId, Pageable pageable);

    long countByAppId(long appId);

    long countByAppIdAndVotedUp(long appId, boolean votedUp);

    @Modifying
    @Transactional
    @Query("update Review r set r.category = null where r.appId = :appId")
    int clearCategories(long appId);

    @Query("""
            select new dev.camitermine.reviews.review.CategoryCount(r.category, count(r))
            from Review r
            where r.appId = :appId and r.category is not null
            group by r.category
            """)
    List<CategoryCount> countByCategory(long appId);

    // --- Consultas para el resumen semanal (reseñas creadas desde una fecha) ---

    long countByAppIdAndCreatedAtGreaterThanEqual(long appId, Instant since);

    long countByAppIdAndVotedUpAndCreatedAtGreaterThanEqual(long appId, boolean votedUp, Instant since);

    @Query("""
            select new dev.camitermine.reviews.review.CategoryCount(r.category, count(r))
            from Review r
            where r.appId = :appId and r.category is not null and r.createdAt >= :since
            group by r.category
            """)
    List<CategoryCount> countByCategorySince(long appId, Instant since);

    List<Review> findByAppIdAndCategoryAndCreatedAtGreaterThanEqual(long appId, ReviewCategory category,
                                                                    Instant since, Pageable pageable);

    List<Review> findByAppIdAndCategoryInAndCreatedAtGreaterThanEqual(long appId, List<ReviewCategory> categories,
                                                                      Instant since, Pageable pageable);

    /** Para la exportación a CSV: todas las reseñas del juego, ordenadas. */
    List<Review> findByAppId(long appId, Sort sort);
}
