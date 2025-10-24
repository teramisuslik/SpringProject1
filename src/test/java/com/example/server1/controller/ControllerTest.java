package com.example.server1.controller;

import com.example.server1.entity.Comment;
import com.example.server1.entity.Role;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.jwt.AuthRequest;
import com.example.server1.jwt.AuthResponse;
import com.example.server1.jwt.JwtTokenUtils;
import com.example.server1.service.TaskService;
import com.example.server1.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @InjectMocks
    private Controller controller;

    private User user;
    private Task task;
    private AuthRequest authRequest;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .build();

        authRequest = new AuthRequest("testuser", "password");

        comment = Comment.builder()
                .id(1L)
                .content("Test Comment")
                .build();
    }

    @Test
    void createUser_ShouldCreateUserAndReturnRedirect() {
        // Given
        when(userService.create(anyString(), anyString())).thenReturn(user);

        // When
        String result = controller.createUser(authRequest);

        // Then
        assertThat(result).isEqualTo("redirect:/login");
        verify(userService).create(authRequest.getUsername(), authRequest.getPassword());
    }

    @Test
    void createAdmin_ShouldCreateAdminAndReturnRedirect() {
        // Given
        when(userService.createAdmin(anyString(), anyString())).thenReturn(user);

        // When
        String result = controller.createAdmin(authRequest);

        // Then
        assertThat(result).isEqualTo("redirect:/login");
        verify(userService).createAdmin(authRequest.getUsername(), authRequest.getPassword());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        // Given
        String token = "jwt-token";
        when(userService.login(anyString(), anyString())).thenReturn(user);
        when(jwtTokenUtils.generateToken(user)).thenReturn(token);

        // When
        ResponseEntity<AuthResponse> result = controller.login(authRequest);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getToken()).isEqualTo(token);
        verify(userService).login(authRequest.getUsername(), authRequest.getPassword());
        verify(jwtTokenUtils).generateToken(user);
    }

    @Test
    void getMainPage_ShouldReturnMainPage() {
        // When
        String result = controller.getMainPage();

        // Then
        assertThat(result).isEqualTo("main");
    }

    @Test
    void getUser_ShouldReturnUser() {
        // Given
        String username = "testuser";
        when(userService.findByUsername(username)).thenReturn(user);

        // When
        User result = controller.getUser(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        verify(userService).findByUsername(username);
    }

    @Test
    void getUsers_ShouldReturnAllUsers() {
        // Given
        List<User> users = Arrays.asList(user);
        when(userService.findAll()).thenReturn(users);

        // When
        List<User> result = controller.getUsers();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
        verify(userService).findAll();
    }

    @Test
    void getUserWithoutTasks_ShouldReturnUserInfo() {
        // Given
        String username = "testuser";
        when(userService.getUserByUsername(username)).thenReturn(user);

        // When
        Map<String, Object> result = controller.getUserWithoutTasks(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("username")).isEqualTo("testuser");
        assertThat(result.get("role")).isEqualTo(Role.USER);
        verify(userService).getUserByUsername(username);
    }

    @Test
    void markTaskAsCompleted_ShouldCallTaskService() {
        // Given
        Long taskId = 1L;
        when(taskService.markTaskAsCompleted(taskId)).thenReturn("Статус изменен");

        // When
        String result = controller.markTaskAsCompleted(taskId);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        verify(taskService).markTaskAsCompleted(taskId);
    }

    @Test
    void markTaskAsInWork_ShouldCallTaskService() {
        // Given
        Long taskId = 1L;
        when(taskService.markTaskAsInWork(taskId)).thenReturn("Статус изменен");

        // When
        String result = controller.markTaskAsInWork(taskId);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        verify(taskService).markTaskAsInWork(taskId);
    }

    @Test
    void markTaskAsOnRework_ShouldCallTaskService() {
        // Given
        Long taskId = 1L;
        when(taskService.markTaskAsOnRework(taskId, comment)).thenReturn("Статус изменен");

        // When
        String result = controller.markTaskAsOnRework(taskId, comment);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        verify(taskService).markTaskAsOnRework(taskId, comment);
    }

    @Test
    void getAllUsers_ShouldReturnAllUsernames() {
        // Given
        List<String> usernames = Arrays.asList("user1", "user2");
        when(userService.findAllUsername()).thenReturn(usernames);

        // When
        List<String> result = controller.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("user1", "user2");
        verify(userService).findAllUsername();
    }

    @Test
    void deleteUser_ShouldDeleteUserAndReturnOk() {
        // Given
        String username = "testuser";
        doNothing().when(userService).deleteUserByUsername(username);

        // When
        ResponseEntity<String> result = controller.deleteUser(username);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("User deleted");
        verify(userService).deleteUserByUsername(username);
    }

    @Test
    void updateTask_ShouldUpdateTaskAndReturnOk() {
        // Given
        when(taskService.updateTask(task)).thenReturn("Задача обновлена");

        // When
        ResponseEntity<String> result = controller.updateTask(task);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Task updated");
        verify(taskService).updateTask(task);
    }
}

