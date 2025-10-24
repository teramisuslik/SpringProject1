package com.example.server1.integrativetest;

import com.example.server1.controller.NotificationProduser;
import com.example.server1.controller.TaskConsumer;
import com.example.server1.entity.Importance;
import com.example.server1.entity.Role;
import com.example.server1.entity.Status;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.service.TaskService;
import com.example.server1.service.UserService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"user-notifications", "admin-notifications"})
@ActiveProfiles("test")
@Transactional
class KafkaIntegrationTest {

    @Autowired
    private NotificationProduser notificationProducer;

    @Autowired
    private TaskConsumer taskConsumer;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    private User testUser;
    private Task testTask;
    private List<String> receivedMessages = new ArrayList<>();
    private CountDownLatch messageLatch = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        receivedMessages.clear();
        messageLatch = new CountDownLatch(1);

        // Создание тестового пользователя
        testUser = userService.create("kafkatestuser", "password");

        // Создание тестовой задачи
        testTask = Task.builder()
                .title("Kafka Test Task")
                .description("Test Description")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .assignee(testUser)
                .build();
    }

    @Test
    void notificationProducer_ShouldSendUserNotification() throws InterruptedException {
        // Given
        String message = "Test notification for user";
        String username = "kafkatestuser";

        // When
        notificationProducer.sendNotificationForUser(message, username);

        // Then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.get(0)).contains(message);
    }

    @Test
    void notificationProducer_ShouldSendAdminNotification() throws InterruptedException {
        // Given
        String message = "Test notification for admin";

        // When
        notificationProducer.sendNotificationForAdmin(message);

        // Then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.get(0)).contains(message);
    }

    @Test
    void taskService_ShouldSendNotificationWhenTaskCompleted() throws InterruptedException {
        // Given
        testTask.setStatus(Status.В_РАБОТЕ);
        testTask = taskService.findById(testTask.getId()).orElse(null);

        // When
        taskService.markTaskAsCompleted(testTask.getId());

        // Then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.get(0)).contains("завершил задачу");
        assertThat(receivedMessages.get(0)).contains("Kafka Test Task");
    }

    @Test
    void taskService_ShouldSendNotificationWhenTaskSentForRework() throws InterruptedException {
        // Given
        testTask.setStatus(Status.ЗАВЕРШЕНА);
        testTask = taskService.findById(testTask.getId()).orElse(null);

        // When
        taskService.markTaskAsOnRework(testTask.getId(), 
            new com.example.server1.entity.Comment(null, "Need rework", testTask));

        // Then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.get(0)).contains("отправили на доработку");
        assertThat(receivedMessages.get(0)).contains("Kafka Test Task");
    }

    @Test
    void taskService_ShouldSendNotificationWhenTaskUpdated() throws InterruptedException {
        // Given
        Task updatedTask = Task.builder()
                .title("Kafka Test Task")
                .description("Updated Description")
                .status(Status.В_РАБОТЕ)
                .importance(Importance.НАДО_ПОТОРОПИТЬСЯ)
                .build();

        // When
        taskService.updateTask(updatedTask);

        // Then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.get(0)).contains("изменена");
        assertThat(receivedMessages.get(0)).contains("Kafka Test Task");
    }

    @Test
    void userService_ShouldSendNotificationWhenTaskAdded() throws InterruptedException {
        // Given
        Task newTask = Task.builder()
                .title("New Kafka Task")
                .description("New Description")
                .status(Status.НЕ_НАЧАТА)
                .importance(Importance.СРОЧНАЯ)
                .deadline(LocalDateTime.now().plusDays(1))
                .build();

        // When
        userService.addTasks("kafkatestuser", newTask);

        // Then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.get(0)).contains("новое задание");
    }

    @Test
    void kafkaConsumer_ShouldReceiveAndProcessMessages() throws InterruptedException {
        // Given
        String testMessage = "Test message for consumer";

        // When
        notificationProducer.sendNotificationForUser(testMessage, "kafkatestuser");

        // Then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.get(0)).isEqualTo(testMessage);
    }

    @Test
    void kafkaIntegration_ShouldHandleMultipleMessages() throws InterruptedException {
        // Given
        messageLatch = new CountDownLatch(3);
        String message1 = "Message 1";
        String message2 = "Message 2";
        String message3 = "Message 3";

        // When
        notificationProducer.sendNotificationForUser(message1, "kafkatestuser");
        notificationProducer.sendNotificationForUser(message2, "kafkatestuser");
        notificationProducer.sendNotificationForUser(message3, "kafkatestuser");

        // Then
        assertThat(messageLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).hasSize(3);
        assertThat(receivedMessages).containsExactlyInAnyOrder(message1, message2, message3);
    }

    @Test
    void kafkaIntegration_ShouldHandleConcurrentMessages() throws InterruptedException {
        // Given
        messageLatch = new CountDownLatch(5);
        int numberOfThreads = 5;

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            final int messageId = i;
            new Thread(() -> {
                notificationProducer.sendNotificationForUser("Concurrent message " + messageId, "kafkatestuser");
            }).start();
        }

        // Then
        assertThat(messageLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).hasSize(5);
    }

    @Test
    void kafkaIntegration_ShouldHandleLargeMessages() throws InterruptedException {
        // Given
        StringBuilder largeMessageBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeMessageBuilder.append("Large message content ").append(i).append(" ");
        }
        String largeMessage = largeMessageBuilder.toString();

        // When
        notificationProducer.sendNotificationForUser(largeMessage, "kafkatestuser");

        // Then
        assertThat(messageLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.get(0)).contains("Large message content");
    }

    @Test
    void kafkaIntegration_ShouldHandleSpecialCharacters() throws InterruptedException {
        // Given
        String specialMessage = "Message with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        String unicodeMessage = "Message with unicode: Привет мир! 🌍";

        // When
        notificationProducer.sendNotificationForUser(specialMessage, "kafkatestuser");
        messageLatch.await(5, TimeUnit.SECONDS);
        
        messageLatch = new CountDownLatch(1);
        notificationProducer.sendNotificationForUser(unicodeMessage, "kafkatestuser");

        // Then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessages).hasSize(2);
        assertThat(receivedMessages.get(0)).isEqualTo(specialMessage);
        assertThat(receivedMessages.get(1)).isEqualTo(unicodeMessage);
    }

    // Kafka Consumer для тестирования
    @KafkaListener(topics = "user-notifications")
    public void listenToUserNotifications(ConsumerRecord<String, String> record) {
        receivedMessages.add(record.value());
        messageLatch.countDown();
    }

    @KafkaListener(topics = "admin-notifications")
    public void listenToAdminNotifications(ConsumerRecord<String, String> record) {
        receivedMessages.add(record.value());
        messageLatch.countDown();
    }
}
