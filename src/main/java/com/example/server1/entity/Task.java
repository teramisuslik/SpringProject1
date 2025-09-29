package com.example.server1.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "tasks")
@JsonIgnoreProperties("assignee")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Importance importance;

    private LocalDateTime deadline;

    @JoinColumn(name = "user_id")
    @ManyToOne
    private User assignee;

    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "task"
    )
    private List<Comment> comments;

}
