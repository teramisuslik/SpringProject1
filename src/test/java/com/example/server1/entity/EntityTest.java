package com.example.server1.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EntityTest {

    @Test
    void userBuilder_ShouldCreateUserWithAllFields() {
        // Given
        String username = "testuser";
        String password = "password";
        Role role = Role.USER;
        List<Task> tasks = new ArrayList<>();

        // When
        User user = User.builder()
                .id(1L)
                .username(username)
                .password(password)
                .role(role)
                .tasks(tasks)
                .build();

        // Then
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getRole()).isEqualTo(role);
        assertThat(user.getTasks()).isEqualTo(tasks);
    }

    @Test
    void userUserDetails_ShouldImplementUserDetailsCorrectly() {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        // When & Then
        assertThat(user.getAuthorities()).hasSize(1);
        assertThat(user.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void taskBuilder_ShouldCreateTaskWithAllFields() {
        // Given
        String title = "Test Task";
        String description = "Test Description";
        Status status = Status.НЕ_НАЧАТА;
        Importance importance = Importance.СРОЧНАЯ;
        LocalDateTime deadline = LocalDateTime.now().plusDays(1);
        User assignee = User.builder().username("testuser").build();
        List<Comment> comments = new ArrayList<>();

        // When
        Task task = Task.builder()
                .id(1L)
                .title(title)
                .description(description)
                .status(status)
                .importance(importance)
                .deadline(deadline)
                .assignee(assignee)
                .comments(comments)
                .build();

        // Then
        assertThat(task.getId()).isEqualTo(1L);
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getDescription()).isEqualTo(description);
        assertThat(task.getStatus()).isEqualTo(status);
        assertThat(task.getImportance()).isEqualTo(importance);
        assertThat(task.getDeadline()).isEqualTo(deadline);
        assertThat(task.getAssignee()).isEqualTo(assignee);
        assertThat(task.getComments()).isEqualTo(comments);
    }

    @Test
    void commentBuilder_ShouldCreateCommentWithAllFields() {
        // Given
        String content = "Test Comment";
        Task task = Task.builder().title("Test Task").build();

        // When
        Comment comment = Comment.builder()
                .id(1L)
                .content(content)
                .task(task)
                .build();

        // Then
        assertThat(comment.getId()).isEqualTo(1L);
        assertThat(comment.getContent()).isEqualTo(content);
        assertThat(comment.getTask()).isEqualTo(task);
    }

    @Test
    void statusEnum_ShouldHaveAllExpectedValues() {
        // When & Then
        assertThat(Status.НЕ_НАЧАТА).isNotNull();
        assertThat(Status.В_РАБОТЕ).isNotNull();
        assertThat(Status.ЗАВЕРШЕНА).isNotNull();
        assertThat(Status.НА_ДОРАБОТКЕ).isNotNull();
    }

    @Test
    void importanceEnum_ShouldHaveAllExpectedValues() {
        // When & Then
        assertThat(Importance.СРОЧНАЯ).isNotNull();
        assertThat(Importance.НАДО_ПОТОРОПИТЬСЯ).isNotNull();
        assertThat(Importance.МОЖЕТ_ПОДОЖДАТЬ).isNotNull();
    }

    @Test
    void roleEnum_ShouldHaveCorrectAuthority() {
        // When & Then
        assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_USER");
        assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void userEqualsAndHashCode_ShouldWorkCorrectly() {
        // Given
        User user1 = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        User user2 = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        User user3 = User.builder()
                .id(2L)
                .username("differentuser")
                .password("password")
                .role(Role.USER)
                .build();

        // When & Then
        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        assertThat(user1).isNotEqualTo(user3);
    }

    @Test
    void taskEqualsAndHashCode_ShouldWorkCorrectly() {
        // Given
        Task task1 = Task.builder()
                .id(1L)
                .title("Test Task")
                .status(Status.НЕ_НАЧАТА)
                .build();

        Task task2 = Task.builder()
                .id(1L)
                .title("Test Task")
                .status(Status.НЕ_НАЧАТА)
                .build();

        Task task3 = Task.builder()
                .id(2L)
                .title("Different Task")
                .status(Status.НЕ_НАЧАТА)
                .build();

        // When & Then
        assertThat(task1).isEqualTo(task2);
        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());
        assertThat(task1).isNotEqualTo(task3);
    }

    @Test
    void userToString_ShouldContainUsername() {
        // Given
        User user = User.builder()
                .username("testuser")
                .role(Role.USER)
                .build();

        // When
        String userString = user.toString();

        // Then
        assertThat(userString).contains("testuser");
    }

    @Test
    void taskToString_ShouldContainTitle() {
        // Given
        Task task = Task.builder()
                .title("Test Task")
                .status(Status.НЕ_НАЧАТА)
                .build();

        // When
        String taskString = task.toString();

        // Then
        assertThat(taskString).contains("Test Task");
    }

    @Test
    void commentToString_ShouldContainContent() {
        // Given
        Comment comment = Comment.builder()
                .content("Test Comment")
                .build();

        // When
        String commentString = comment.toString();

        // Then
        assertThat(commentString).contains("Test Comment");
    }
}

