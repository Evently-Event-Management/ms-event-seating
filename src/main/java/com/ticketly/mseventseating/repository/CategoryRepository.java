package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find a category by its name
     *
     * @param name the name of the category
     * @return the category if found
     */
    Optional<Category> findByName(String name);

    /**
     * Find all categories that don't have a parent (top-level categories)
     *
     * @return list of top-level categories
     */
    List<Category> findByParentIsNull();

    /**
     * Find all categories by their IDs
     *
     * @param ids the IDs of the categories
     * @return list of categories with the given IDs
     */
    @Override
    @NonNull
    List<Category> findAllById(@NonNull Iterable<UUID> ids);
}
