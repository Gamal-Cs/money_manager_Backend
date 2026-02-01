// src/main/java/com/moneymanager/repository/CategoryRepository.java
package com.moneymanager.repository;

import com.moneymanager.model.Category;
import com.moneymanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserId(Long userId);
    List<Category> findByUserIdAndType(Long userId, Category.Type type);
    Optional<Category> findByIdAndUserId(Long id, Long userId);
    boolean existsByNameAndUserId(String name, Long userId);
    List<Category> findByUserIdAndParentCategoryIsNull(Long userId);
    List<Category> findByParentCategoryId(Long parentId);
}