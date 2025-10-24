package com.example.server1.integrativetest;

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
@Transactional
class EndToEndIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepositopy commentRepository;

    private String baseUrl;
    private User testUser;
    private User adminUser;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        
        // Очистка данных
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Создание тестового пользователя
        testUser = userService.create("e2etestuser", "password");
        
        // Создание тестового администратора
        adminUser = userService.createAdmin("e2etestadmin", "password");

        // Получение токенов
        userToken = getAuthToken("e2etestuser", "password");
        adminToken = getAuthToken("e2etestadmin", "password");
    }

    @Test
    void completeTaskWorkflow_ShouldWorkEndToEnd() {
        // 1. Регистрация нового пользователя
        AuthRequest newUserRequest = new AuthRequest("newuser", "password");
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                baseUrl + "/register", newUserRequest, String.class);
        assertThat(registerResponse.getStatusCode().value()).isEqualTo(200);

        // 2. Вход в систему
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl + "/login", newUserRequest, String.class);
        assertThat(loginResponse.getStatusCode().value()).isEqualTo(200);

        // 3. Создание задачи (через сервис)
        Task newTask = Task.builder()
                .title("E2E Test Task")
                .description("End-to-end test task")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .assignee(testUser)
                .build();
        taskRepository.save(newTask);

        // 4. Получение пользователя с задачами
        User userWithTasks = userRepository.findByUsername("e2etestuser").orElse(null);
        assertThat(userWithTasks).isNotNull();

        // 5. Отметка задачи как в работе
        String inWorkResponse = taskService.markTaskAsInWork(newTask.getId());
        assertThat(inWorkResponse).isEqualTo("Статус изменен");

        // 6. Проверка изменения статуса в базе данных
        Task updatedTask = taskRepository.findById(newTask.getId()).orElse(null);
        assertThat(updatedTask).isNotNull();
        assertThat(updatedTask.getStatus()).isEqualTo(Status.В_РАБОТЕ);

        // 7. Отметка задачи как завершенной
        String completedResponse = taskService.markTaskAsCompleted(newTask.getId());
        assertThat(completedResponse).isEqualTo("Статус изменен");

        // 8. Проверка финального статуса
        Task finalTask = taskRepository.findById(newTask.getId()).orElse(null);
        assertThat(finalTask).isNotNull();
        assertThat(finalTask.getStatus()).isEqualTo(Status.ЗАВЕРШЕНА);
    }

    @Test
    void adminTaskManagement_ShouldWorkEndToEnd() {
        // 1. Создание задачи
        Task task = Task.builder()
                .title("Admin Management Task")
                .description("Task for admin management")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.НАДО_ПОТОРОПИТЬСЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .assignee(testUser)
                .build();
        task = taskRepository.save(task);

        // 2. Пользователь отмечает задачу как завершенную
        task.setStatus(Status.В_РАБОТЕ);
        taskRepository.save(task);
        taskService.markTaskAsCompleted(task.getId());

        // 3. Администратор отправляет задачу на доработку
        Comment reworkComment = Comment.builder()
                .content("Needs improvement")
                .task(task)
                .build();

        String reworkResponse = taskService.markTaskAsOnRework(task.getId(), reworkComment);
        assertThat(reworkResponse).isEqualTo("Статус изменен");

        // 4. Проверка статуса и комментария
        Task reworkTask = taskRepository.findById(task.getId()).orElse(null);
        assertThat(reworkTask).isNotNull();
        assertThat(reworkTask.getStatus()).isEqualTo(Status.НА_ДОРАБОТКЕ);
        assertThat(reworkTask.getComments()).hasSize(1);
        assertThat(reworkTask.getComments().get(0).getContent()).isEqualTo("Needs improvement");

        // 5. Администратор обновляет задачу
        Task updatedTask = Task.builder()
                .title("Admin Management Task")
                .description("Updated description")
                .status(Status.В_РАБОТЕ)
                .importance(Importance.СРОЧНАЯ)
                .build();

        String updateResponse = taskService.updateTask(updatedTask);
        assertThat(updateResponse).isEqualTo("Задача обновлена");

        // 6. Проверка обновлений
        Task finalTask = taskRepository.findById(task.getId()).orElse(null);
        assertThat(finalTask).isNotNull();
        assertThat(finalTask.getDescription()).isEqualTo("Updated description");
        assertThat(finalTask.getImportance()).isEqualTo(Importance.СРОЧНАЯ);
    }

    @Test
    void userManagement_ShouldWorkEndToEnd() {
        // 1. Получение списка всех пользователей (администратор)
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.set("Authorization", "Bearer " + adminToken);

        ResponseEntity<List> usersResponse = restTemplate.exchange(
                baseUrl + "/allusers",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                List.class
        );
        assertThat(usersResponse.getStatusCode().value()).isEqualTo(200);

        // 2. Получение информации о конкретном пользователе
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set("Authorization", "Bearer " + userToken);

        ResponseEntity<User> userResponse = restTemplate.exchange(
                baseUrl + "/user?username=e2etestuser",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                User.class
        );
        assertThat(userResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(userResponse.getBody()).isNotNull();
        assertThat(userResponse.getBody().getUsername()).isEqualTo("e2etestuser");

        // 3. Получение пользователя без задач
        ResponseEntity<Object> userWithoutTasksResponse = restTemplate.exchange(
                baseUrl + "/userwithouttasks?username=e2etestuser",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                Object.class
        );
        assertThat(userWithoutTasksResponse.getStatusCode().value()).isEqualTo(200);

        // 4. Удаление пользователя (администратор)
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl + "/deleteuser/e2etestuser",
                HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders),
                String.class
        );
        assertThat(deleteResponse.getStatusCode().value()).isEqualTo(200);

        // 5. Проверка, что пользователь удален
        User deletedUser = userRepository.findByUsername("e2etestuser").orElse(null);
        assertThat(deletedUser).isNull();
    }

    @Test
    void taskCommentWorkflow_ShouldWorkEndToEnd() {
        // 1. Создание задачи
        Task task = Task.builder()
                .title("Comment Test Task")
                .description("Task for comment testing")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .assignee(testUser)
                .build();
        task = taskRepository.save(task);

        // 2. Добавление комментария
        Comment comment = Comment.builder()
                .content("First comment")
                .task(task)
                .build();
        comment = commentRepository.save(comment);

        // 3. Проверка связи задачи и комментария
        Task taskWithComment = taskRepository.findById(task.getId()).orElse(null);
        assertThat(taskWithComment).isNotNull();
        assertThat(taskWithComment.getComments()).hasSize(1);
        assertThat(taskWithComment.getComments().get(0).getContent()).isEqualTo("First comment");

        // 4. Добавление второго комментария
        Comment secondComment = Comment.builder()
                .content("Second comment")
                .task(task)
                .build();
        commentRepository.save(secondComment);

        // 5. Проверка всех комментариев
        List<Comment> allComments = commentRepository.findByTask(task);
        assertThat(allComments).hasSize(2);
        assertThat(allComments).extracting(Comment::getContent)
                .containsExactlyInAnyOrder("First comment", "Second comment");

        // 6. Удаление задачи (должно удалить комментарии каскадно)
        taskRepository.delete(task);

        // 7. Проверка, что комментарии удалены
        List<Comment> remainingComments = commentRepository.findByTask(task);
        assertThat(remainingComments).isEmpty();
    }

    @Test
    void concurrentUserOperations_ShouldWorkEndToEnd() throws InterruptedException {
        // 1. Создание нескольких пользователей одновременно
        int numberOfUsers = 5;
        CountDownLatch latch = new CountDownLatch(numberOfUsers);

        for (int i = 0; i < numberOfUsers; i++) {
            final int userIndex = i;
            new Thread(() -> {
                try {
                    User user = userService.create("concurrentuser" + userIndex, "password");
                    assertThat(user).isNotNull();
                    assertThat(user.getUsername()).isEqualTo("concurrentuser" + userIndex);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 2. Ожидание завершения всех операций
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        // 3. Проверка, что все пользователи созданы
        for (int i = 0; i < numberOfUsers; i++) {
            User user = userRepository.findByUsername("concurrentuser" + i).orElse(null);
            assertThat(user).isNotNull();
        }
    }

    @Test
    void dataConsistency_ShouldBeMaintainedEndToEnd() {
        // 1. Создание пользователя
        User user = userService.create("consistencyuser", "password");
        assertThat(user).isNotNull();

        // 2. Создание задачи для пользователя
        Task task = Task.builder()
                .title("Consistency Test Task")
                .description("Task for consistency testing")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .assignee(user)
                .build();
        task = taskRepository.save(task);

        // 3. Проверка связей
        assertThat(task.getAssignee().getId()).isEqualTo(user.getId());

        // 4. Обновление задачи
        task.setStatus(Status.В_РАБОТЕ);
        taskRepository.save(task);

        // 5. Проверка, что изменения сохранены
        Task updatedTask = taskRepository.findById(task.getId()).orElse(null);
        assertThat(updatedTask).isNotNull();
        assertThat(updatedTask.getStatus()).isEqualTo(Status.В_РАБОТЕ);
        assertThat(updatedTask.getAssignee().getId()).isEqualTo(user.getId());

        // 6. Удаление пользователя
        userService.deleteUserByUsername("consistencyuser");

        // 7. Проверка, что связанные данные удалены
        User deletedUser = userRepository.findByUsername("consistencyuser").orElse(null);
        assertThat(deletedUser).isNull();

        Task orphanedTask = taskRepository.findById(task.getId()).orElse(null);
        assertThat(orphanedTask).isNull();
    }

    @Test
    void errorHandling_ShouldWorkEndToEnd() {
        // 1. Попытка входа с неверными данными
        AuthRequest invalidRequest = new AuthRequest("nonexistent", "wrongpassword");
        ResponseEntity<String> invalidLoginResponse = restTemplate.postForEntity(
                baseUrl + "/login", invalidRequest, String.class);
        assertThat(invalidLoginResponse.getStatusCode().value()).isEqualTo(500);

        // 2. Попытка доступа к защищенному ресурсу без токена
        ResponseEntity<String> unauthorizedResponse = restTemplate.getForEntity(
                baseUrl + "/user?username=e2etestuser", String.class);
        assertThat(unauthorizedResponse.getStatusCode().value()).isEqualTo(401);

        // 3. Попытка выполнения операции с несуществующей задачей
        String result = taskService.markTaskAsInWork(999L);
        assertThat(result).contains("задача не найдена");

        // 4. Попытка получения несуществующего пользователя
        User nonExistentUser = userRepository.findByUsername("nonexistent").orElse(null);
        assertThat(nonExistentUser).isNull();
    }

    private String getAuthToken(String username, String password) {
        try {
            AuthRequest authRequest = new AuthRequest(username, password);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/login", 
                    authRequest, 
                    String.class
            );
            
            if (response.getStatusCode().value() == 200 && response.getBody() != null) {
                // Простое извлечение токена (в реальном приложении нужно парсить JSON)
                return "mock-token-" + username;
            }
        } catch (Exception e) {
            // В случае ошибки возвращаем mock токен
        }
        return "mock-token-" + username;
    }
}
