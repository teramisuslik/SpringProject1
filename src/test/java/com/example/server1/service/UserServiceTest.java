package com.example.server1.service;

import com.example.server1.controller.NotificationProduser;
import com.example.server1.entity.Importance;
import com.example.server1.entity.Role;
import com.example.server1.entity.Status;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.exeptions.NotFoundExeption;
import com.example.server1.repository.CommentRepositopy;
import com.example.server1.repository.TaskRepository;
import com.example.server1.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NotificationProduser notificationProduser;

    @Mock
    private CommentRepositopy commentRepositopy;

    @InjectMocks
    private UserService userService;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .role(Role.USER)
                .tasks(new ArrayList<>())
                .build();

        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    void create_ShouldCreateUserWithUserRole() {
        // Given
        String username = "newuser";
        String password = "password";
        String encodedPassword = "encodedPassword";

        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User result = userService.create(username, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo(encodedPassword);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAdmin_ShouldCreateUserWithAdminRole() {
        // Given
        String username = "admin";
        String password = "password";
        String encodedPassword = "encodedPassword";

        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User result = userService.createAdmin(username, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo(encodedPassword);
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnUser() {
        // Given
        String username = "testuser";
        String password = "password";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

        // When
        User result = userService.login(username, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        verify(passwordEncoder).matches(password, user.getPassword());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowException() {
        // Given
        String username = "testuser";
        String password = "wrongpassword";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.login(username, password))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("при попытке входа что-то пошло не так");
    }

    @Test
    void login_WithNonExistentUser_ShouldThrowException() {
        // Given
        String username = "nonexistent";
        String password = "password";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.login(username, password))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("при попытке входа что-то пошло не так");
    }

    @Test
    void addTasks_WithValidTask_ShouldAddTaskToUser() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User result = userService.addTasks(username, task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTasks()).contains(task);
        assertThat(task.getStatus()).isEqualTo(Status.НЕ_НАЧАТА);
        assertThat(task.getAssignee()).isEqualTo(user);
        verify(taskRepository).save(task);
        verify(userRepository).save(user);
        verify(notificationProduser).sendNotificationForUser(anyString(), eq(username));
    }

    @Test
    void addTasks_WithNullTask_ShouldThrowException() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.addTasks(username, null))
                .isInstanceOf(NotFoundExeption.class)
                .hasMessage("пустой список задач");
    }

    @Test
    void addTasks_WithNonExistentUser_ShouldThrowException() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.addTasks(username, task))
                .isInstanceOf(NotFoundExeption.class)
                .hasMessage("пользователь не найден");
    }

    @Test
    void findByUsername_WhenUserExists_ShouldReturnUser() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        User result = userService.findByUsername(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
    }

    @Test
    void findByUsername_WhenUserNotExists_ShouldThrowException() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findByUsername(username))
                .isInstanceOf(NotFoundExeption.class)
                .hasMessage("такого пользователя нет");
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        // Given
        List<User> users = Arrays.asList(user);
        when(userRepository.findAllByRole(Role.USER)).thenReturn(users);

        // When
        List<User> result = userService.findAll();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUsername_WhenUserExists_ShouldReturnUsername() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        String result = userService.getUsername(username);

        // Then
        assertThat(result).isEqualTo(username);
    }

    @Test
    void getRole_WhenUserExists_ShouldReturnRole() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        Role result = userService.getRole(username);

        // Then
        assertThat(result).isEqualTo(Role.USER);
    }

    @Test
    void deleteUserByUsername_ShouldDeleteUserAndTasks() {
        // Given
        String username = "testuser";
        when(userRepository.getUserByUsername(username)).thenReturn(user);

        // When
        userService.deleteUserByUsername(username);

        // Then
        verify(taskRepository).deleteByUserId(user.getId());
        verify(userRepository).deleteByUsername(username);
    }

    @Test
    void deleteTask_ShouldDeleteTaskFromUser() {
        // Given
        String username = "testuser";
        Long taskId = 1L;
        user.setTasks(Arrays.asList(task));
        
        when(userRepository.getUserByUsername(username)).thenReturn(user);

        // When
        userService.deleteTask(username, taskId);

        // Then
        verify(commentRepositopy).deleteAllByTask(task);
        verify(taskRepository).deleteTaskById(taskId);
        verify(notificationProduser).sendNotificationForUser(anyString(), eq(username));
    }

    @Test
    void getUserByUsername_ShouldReturnUser() {
        // Given
        String username = "testuser";
        when(userRepository.getUserByUsername(username)).thenReturn(user);

        // When
        User result = userService.getUserByUsername(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
    }

    @Test
    void findAllUsername_ShouldReturnAllUsernames() {
        // Given
        List<User> users = Arrays.asList(user);
        when(userRepository.findAllByRole(Role.USER)).thenReturn(users);

        // When
        List<String> result = userService.findAllUsername();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("testuser");
    }
}

