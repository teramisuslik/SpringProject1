package com.example.server1.repository;

import com.example.server1.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepositopy extends JpaRepository<Comment,Long> {
}
