package com.interview.platform.repository;

import com.interview.platform.enums.Role;
import com.interview.platform.models.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true ORDER BY u.createdAt DESC")
    List<User> findActiveByRole(Role role);
}
