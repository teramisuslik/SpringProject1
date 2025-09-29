package com.example.server1.repository;

import com.example.server1.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task,Long> {
    Optional<Task> findTaskByTitle(String title);

    @Modifying
    @Query(value = "DELETE FROM tasks WHERE user_id = :user_id", nativeQuery = true)
    int deleteByUserId(@Param("user_id") Long user_id);

    Optional<Task> findByTitle (String title);

    void deleteTaskById(Long id);
}
