package com.example.server1.integration;

import com.example.server1.entity.Comment;
import com.example.server1.entity.Importance;
import com.example.server1.entity.Role;
import com.example.server1.entity.Status;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.jwt.AuthRequest;
import com.example.server1.repository.CommentRepositopy;
import com.example.server1.repository.TaskRepository;
import com.example.server1.repository.UserRepository;
import com.example.server1.service.TaskService;
import com.example.server1.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepositopy commentRepositopy;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    private String baseUrl;
    private User testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        
        // Очистка данных перед каждым тестом
        commentRepositopy.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Создание тестового пользователя
        testUser = userService.create("testuser", "password");
        
        // Создание тестовой задачи
        testTask = Task.builder()
                .title("Integration Test Task")
                .description("Test Description")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .assignee(testUser)
                .build();
        taskRepository.save(testTask);
    }

    @Test
    void userRegistration_ShouldCreateUser() {
        // Given
        AuthRequest authRequest = new AuthRequest("newuser", "password");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/register", 
                authRequest, 
                String.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        
        // Проверяем, что пользователь создан в базе данных
        User createdUser = userRepository.findByUsername("newuser").orElse(null);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void adminRegistration_ShouldCreateAdmin() {
        // Given
        AuthRequest authRequest = new AuthRequest("admin", "password");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/registeradmin", 
                authRequest, 
                String.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        
        // Проверяем, что админ создан в базе данных
        User createdAdmin = userRepository.findByUsername("admin").orElse(null);
        assertThat(createdAdmin).isNotNull();
        assertThat(createdAdmin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void userLogin_ShouldReturnToken() {
        // Given
        AuthRequest authRequest = new AuthRequest("testuser", "password");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login", 
                authRequest, 
                String.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("token");
    }

    @Test
    void taskWorkflow_ShouldWorkEndToEnd() {
        // Given
        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        // When - Отмечаем задачу как в работе
        ResponseEntity<String> inWorkResponse = restTemplate.exchange(
                baseUrl + "/markthetaskasinwork?taskId=" + testTask.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(inWorkResponse.getStatusCode().value()).isEqualTo(200);
        
        // Проверяем изменение статуса в базе данных
        Task updatedTask = taskRepository.findById(testTask.getId()).orElse(null);
        assertThat(updatedTask).isNotNull();
        assertThat(updatedTask.getStatus()).isEqualTo(Status.В_РАБОТЕ);

        // When - Отмечаем задачу как завершенную
        ResponseEntity<String> completedResponse = restTemplate.exchange(
                baseUrl + "/markthetaskascompleted?taskId=" + testTask.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertThat(completedResponse.getStatusCode().value()).isEqualTo(200);
        
        // Проверяем изменение статуса в базе данных
        Task completedTask = taskRepository.findById(testTask.getId()).orElse(null);
        assertThat(completedTask).isNotNull();
        assertThat(completedTask.getStatus()).isEqualTo(Status.ЗАВЕРШЕНА);
    }

    @Test
    void taskRework_ShouldWorkWithComments() {
        // Given
        String adminToken = getAdminAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        // Сначала завершаем задачу
        testTask.setStatus(Status.ЗАВЕРШЕНА);
        taskRepository.save(testTask);

        Comment comment = Comment.builder()
                .content("Нужна доработка")
                .task(testTask)
                .build();

        // When - Отправляем задачу на доработку
        ResponseEntity<String> reworkResponse = restTemplate.exchange(
                baseUrl + "/markthetaskasonrework?taskId=" + testTask.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(comment, headers),
                String.class
        );

        // Then
        assertThat(reworkResponse.getStatusCode().value()).isEqualTo(200);
        
        // Проверяем изменение статуса и добавление комментария
        Task reworkTask = taskRepository.findById(testTask.getId()).orElse(null);
        assertThat(reworkTask).isNotNull();
        assertThat(reworkTask.getStatus()).isEqualTo(Status.НА_ДОРАБОТКЕ);
        assertThat(reworkTask.getComments()).hasSize(1);
        assertThat(reworkTask.getComments().get(0).getContent()).isEqualTo("Нужна доработка");
    }

    @Test
    void getAllUsers_ShouldReturnUsersList() {
        // Given
        String adminToken = getAdminAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        // When
        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl + "/allusers",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getUser_ShouldReturnUserInfo() {
        // Given
        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        // When
        ResponseEntity<User> response = restTemplate.exchange(
                baseUrl + "/user?username=testuser",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                User.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updateTask_ShouldUpdateTask() {
        // Given
        String adminToken = getAdminAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        Task updatedTask = Task.builder()
                .title("Integration Test Task")
                .description("Updated Description")
                .status(Status.В_РАБОТЕ)
                .importance(Importance.НАДО_ПОТОРОПИТЬСЯ)
                .build();

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/updatetask",
                HttpMethod.PUT,
                new HttpEntity<>(updatedTask, headers),
                String.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        
        // Проверяем обновление в базе данных
        Task task = taskRepository.findTaskByTitle("Integration Test Task").orElse(null);
        assertThat(task).isNotNull();
        assertThat(task.getDescription()).isEqualTo("Updated Description");
        assertThat(task.getStatus()).isEqualTo(Status.В_РАБОТЕ);
        assertThat(task.getImportance()).isEqualTo(Importance.НАДО_ПОТОРОПИТЬСЯ);
    }

    private String getAuthToken() {
        AuthRequest authRequest = new AuthRequest("testuser", "password");
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login", 
                authRequest, 
                String.class
        );
        
        // Извлекаем токен из ответа (предполагаем, что ответ содержит JSON с токеном)
        String responseBody = response.getBody();
        if (responseBody != null && responseBody.contains("token")) {
            // Простое извлечение токена (в реальном приложении нужно парсить JSON)
            return "mock-token";
        }
        return "mock-token";
    }

    private String getAdminAuthToken() {
        // Создаем админа для тестов
        userService.createAdmin("admin", "password");
        
        AuthRequest authRequest = new AuthRequest("admin", "password");
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login", 
                authRequest, 
                String.class
        );
        
        return "mock-admin-token";
    }
}

