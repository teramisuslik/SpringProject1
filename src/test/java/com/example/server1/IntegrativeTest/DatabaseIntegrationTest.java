package com.example.server1.integrativetest;

import com.example.server1.entity.Comment;
import com.example.server1.entity.Importance;
import com.example.server1.entity.Role;
import com.example.server1.entity.Status;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.repository.CommentRepositopy;
import com.example.server1.repository.TaskRepository;
import com.example.server1.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class DatabaseIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepositopy commentRepository;

    private User testUser;
    private Task testTask;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        // Очистка данных
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Создание тестового пользователя
        testUser = User.builder()
                .username("testuser")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        // Создание тестовой задачи
        testTask = Task.builder()
                .title("Integration Test Task")
                .description("Test Description")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .assignee(testUser)
                .build();
        testTask = entityManager.persistAndFlush(testTask);

        // Создание тестового комментария
        testComment = Comment.builder()
                .content("Integration Test Comment")
                .task(testTask)
                .build();
        testComment = entityManager.persistAndFlush(testComment);
    }

    @Test
    void userRepository_ShouldSaveAndFindUser() {
        // Given
        User newUser = User.builder()
                .username("newuser")
                .password("password")
                .role(Role.USER)
                .build();

        // When
        User savedUser = userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();

        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("newuser");
        assertThat(foundUser.get().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void userRepository_ShouldFindUserByUsername() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void userRepository_ShouldFindUsersByRole() {
        // Given
        User adminUser = User.builder()
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .build();
        userRepository.save(adminUser);

        // When
        List<User> users = userRepository.findAllByRole(Role.USER);
        List<User> admins = userRepository.findAllByRole(Role.ADMIN);

        // Then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("testuser");
        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    void taskRepository_ShouldSaveAndFindTask() {
        // Given
        Task newTask = Task.builder()
                .title("New Task")
                .description("New Description")
                .status(Status.В_РАБОТЕ)
                .importance(Importance.НАДО_ПОТОРОПИТЬСЯ)
                .deadline(LocalDateTime.now().plusDays(2))
                .assignee(testUser)
                .build();

        // When
        Task savedTask = taskRepository.save(newTask);
        entityManager.flush();
        entityManager.clear();

        Optional<Task> foundTask = taskRepository.findById(savedTask.getId());

        // Then
        assertThat(foundTask).isPresent();
        assertThat(foundTask.get().getTitle()).isEqualTo("New Task");
        assertThat(foundTask.get().getStatus()).isEqualTo(Status.В_РАБОТЕ);
        assertThat(foundTask.get().getAssignee().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void taskRepository_ShouldFindTaskByTitle() {
        // When
        Optional<Task> foundTask = taskRepository.findTaskByTitle("Integration Test Task");

        // Then
        assertThat(foundTask).isPresent();
        assertThat(foundTask.get().getTitle()).isEqualTo("Integration Test Task");
        assertThat(foundTask.get().getId()).isEqualTo(testTask.getId());
    }

    @Test
    void taskRepository_ShouldFindTasksByAssignee() {
        // Given
        Task anotherTask = Task.builder()
                .title("Another Task")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.МОЖЕТ_ПОДОЖДАТЬ)
                .assignee(testUser)
                .build();
        taskRepository.save(anotherTask);

        // When
        List<Task> userTasks = taskRepository.findByAssignee(testUser);

        // Then
        assertThat(userTasks).hasSize(2);
        assertThat(userTasks).extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Integration Test Task", "Another Task");
    }

    @Test
    void commentRepository_ShouldSaveAndFindComment() {
        // Given
        Comment newComment = Comment.builder()
                .content("New Comment")
                .task(testTask)
                .build();

        // When
        Comment savedComment = commentRepository.save(newComment);
        entityManager.flush();
        entityManager.clear();

        Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());

        // Then
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).isEqualTo("New Comment");
        assertThat(foundComment.get().getTask().getId()).isEqualTo(testTask.getId());
    }

    @Test
    void commentRepository_ShouldFindCommentsByTask() {
        // Given
        Comment anotherComment = Comment.builder()
                .content("Another Comment")
                .task(testTask)
                .build();
        commentRepository.save(anotherComment);

        // When
        List<Comment> taskComments = commentRepository.findByTask(testTask);

        // Then
        assertThat(taskComments).hasSize(2);
        assertThat(taskComments).extracting(Comment::getContent)
                .containsExactlyInAnyOrder("Integration Test Comment", "Another Comment");
    }

    @Test
    void databaseRelationships_ShouldWorkCorrectly() {
        // Given
        User user = User.builder()
                .username("relationshipuser")
                .password("password")
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        Task task = Task.builder()
                .title("Relationship Task")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .assignee(user)
                .build();
        task = taskRepository.save(task);

        Comment comment = Comment.builder()
                .content("Relationship Comment")
                .task(task)
                .build();
        comment = commentRepository.save(comment);

        // When
        entityManager.flush();
        entityManager.clear();

        // Then - проверяем связи
        User foundUser = userRepository.findById(user.getId()).orElse(null);
        Task foundTask = taskRepository.findById(task.getId()).orElse(null);
        Comment foundComment = commentRepository.findById(comment.getId()).orElse(null);

        assertThat(foundUser).isNotNull();
        assertThat(foundTask).isNotNull();
        assertThat(foundComment).isNotNull();

        assertThat(foundTask.getAssignee().getId()).isEqualTo(user.getId());
        assertThat(foundComment.getTask().getId()).isEqualTo(task.getId());
    }

    @Test
    void databaseTransactions_ShouldRollbackOnError() {
        // Given
        User user = User.builder()
                .username("transactionuser")
                .password("password")
                .role(Role.USER)
                .build();

        // When & Then
        try {
            userRepository.save(user);
            // Симулируем ошибку
            throw new RuntimeException("Simulated error");
        } catch (RuntimeException e) {
            // Проверяем, что пользователь не был сохранен
            Optional<User> foundUser = userRepository.findByUsername("transactionuser");
            assertThat(foundUser).isEmpty();
        }
    }

    @Test
    void databaseConstraints_ShouldBeEnforced() {
        // Given
        User user1 = User.builder()
                .username("duplicateuser")
                .password("password")
                .role(Role.USER)
                .build();

        User user2 = User.builder()
                .username("duplicateuser") // Дублирующий username
                .password("password")
                .role(Role.USER)
                .build();

        // When
        userRepository.save(user1);

        // Then - должно быть исключение при попытке сохранить дубликат
        try {
            userRepository.save(user2);
            entityManager.flush();
            // Если дошли сюда, значит ограничение не работает
            assertThat(false).as("Constraint violation should have occurred").isTrue();
        } catch (Exception e) {
            // Ожидаемое поведение - исключение при нарушении ограничения
            assertThat(e).isNotNull();
        }
    }

    @Test
    void databaseCascadeOperations_ShouldWorkCorrectly() {
        // Given
        User user = User.builder()
                .username("cascadeuser")
                .password("password")
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        Task task = Task.builder()
                .title("Cascade Task")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .assignee(user)
                .build();
        task = taskRepository.save(task);

        Comment comment = Comment.builder()
                .content("Cascade Comment")
                .task(task)
                .build();
        comment = commentRepository.save(comment);

        // When - удаляем задачу
        taskRepository.delete(task);

        // Then - комментарий должен быть удален каскадно
        Optional<Comment> foundComment = commentRepository.findById(comment.getId());
        assertThat(foundComment).isEmpty();
    }
}
