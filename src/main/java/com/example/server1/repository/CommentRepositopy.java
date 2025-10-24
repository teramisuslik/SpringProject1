package com.example.server1.repository;

import com.example.server1.entity.Comment;
import com.example.server1.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepositopy extends JpaRepository<Comment,Long> {

    void deleteAllByTask(Task task);
}
