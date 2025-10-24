package com.example.server1;

import com.example.server1.entity.Importance;
import com.example.server1.entity.Role;
import com.example.server1.entity.Status;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.repository.TaskRepository;
import com.example.server1.repository.UserRepository;
import com.example.server1.service.TaskService;
import com.example.server1.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        // Очистка данных перед каждым тестом
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Создание тестового пользователя
        testUser = userService.create("testuser", "password");
        
        // Создание тестовой задачи
        testTask = Task.builder()
                .title("Test Task")
                .description("Test Description")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .assignee(testUser)
                .build();
        taskRepository.save(testTask);
    }

    @Test
    void contextLoads() {
        // Проверяем, что Spring контекст загружается корректно
        assertThat(userService).isNotNull();
        assertThat(taskService).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(taskRepository).isNotNull();
    }

    @Test
    void userService_ShouldCreateUser() {
        // Given
        String username = "newuser";
        String password = "password";

        // When
        User createdUser = userService.create(username, password);

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo(username);
        assertThat(createdUser.getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches(password, createdUser.getPassword())).isTrue();
    }

    @Test
    void userService_ShouldCreateAdmin() {
        // Given
        String username = "admin";
        String password = "password";

        // When
        User createdAdmin = userService.createAdmin(username, password);

        // Then
        assertThat(createdAdmin).isNotNull();
        assertThat(createdAdmin.getUsername()).isEqualTo(username);
        assertThat(createdAdmin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches(password, createdAdmin.getPassword())).isTrue();
    }

    @Test
    void userService_ShouldLoginUser() {
        // Given
        String username = "testuser";
        String password = "password";

        // When
        User loggedInUser = userService.login(username, password);

        // Then
        assertThat(loggedInUser).isNotNull();
        assertThat(loggedInUser.getUsername()).isEqualTo(username);
    }

    @Test
    void taskService_ShouldMarkTaskAsInWork() {
        // Given
        Long taskId = testTask.getId();

        // When
        String result = taskService.markTaskAsInWork(taskId);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        
        Task updatedTask = taskRepository.findById(taskId).orElse(null);
        assertThat(updatedTask).isNotNull();
        assertThat(updatedTask.getStatus()).isEqualTo(Status.В_РАБОТЕ);
    }

    @Test
    void taskService_ShouldMarkTaskAsCompleted() {
        // Given
        Long taskId = testTask.getId();
        testTask.setStatus(Status.В_РАБОТЕ);
        taskRepository.save(testTask);

        // When
        String result = taskService.markTaskAsCompleted(taskId);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        
        Task updatedTask = taskRepository.findById(taskId).orElse(null);
        assertThat(updatedTask).isNotNull();
        assertThat(updatedTask.getStatus()).isEqualTo(Status.ЗАВЕРШЕНА);
    }

    @Test
    void taskService_ShouldUpdateTask() {
        // Given
        Task updatedTask = Task.builder()
                .title("Test Task")
                .description("Updated Description")
                .status(Status.В_РАБОТЕ)
                .importance(Importance.НАДО_ПОТОРОПИТЬСЯ)
                .build();

        // When
        String result = taskService.updateTask(updatedTask);

        // Then
        assertThat(result).isEqualTo("Задача обновлена");
        
        Task task = taskRepository.findTaskByTitle("Test Task").orElse(null);
        assertThat(task).isNotNull();
        assertThat(task.getDescription()).isEqualTo("Updated Description");
        assertThat(task.getStatus()).isEqualTo(Status.В_РАБОТЕ);
        assertThat(task.getImportance()).isEqualTo(Importance.НАДО_ПОТОРОПИТЬСЯ);
    }

    @Test
    void userRepository_ShouldFindUserByUsername() {
        // Given
        String username = "testuser";

        // When
        User foundUser = userRepository.findByUsername(username).orElse(null);

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo(username);
    }

    @Test
    void taskRepository_ShouldFindTaskById() {
        // Given
        Long taskId = testTask.getId();

        // When
        Task foundTask = taskRepository.findById(taskId).orElse(null);

        // Then
        assertThat(foundTask).isNotNull();
        assertThat(foundTask.getTitle()).isEqualTo("Test Task");
    }

    @Test
    void passwordEncoder_ShouldEncodePassword() {
        // Given
        String rawPassword = "password";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    void user_ShouldHaveCorrectAuthorities() {
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
    void task_ShouldHaveCorrectRelationships() {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .tasks(new ArrayList<>())
                .build();

        Task task = Task.builder()
                .title("Test Task")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .assignee(user)
                .comments(new ArrayList<>())
                .build();

        // When
        user.getTasks().add(task);

        // Then
        assertThat(task.getAssignee()).isEqualTo(user);
        assertThat(user.getTasks()).contains(task);
        assertThat(task.getComments()).isEmpty();
    }
}
