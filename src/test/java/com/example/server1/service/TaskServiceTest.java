package com.example.server1.service;

import com.example.server1.controller.NotificationProduser;
import com.example.server1.entity.Comment;
import com.example.server1.entity.Importance;
import com.example.server1.entity.Status;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.entity.Role;
import com.example.server1.exeptions.NotFoundExeption;
import com.example.server1.repository.CommentRepositopy;
import com.example.server1.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CommentRepositopy commentRepositopy;

    @Mock
    private NotificationProduser notificationProduser;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private User user;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
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
                .assignee(user)
                .comments(new ArrayList<>())
                .build();

        comment = Comment.builder()
                .id(1L)
                .content("Test Comment")
                .task(task)
                .build();
    }

    @Test
    void findById_WhenTaskExists_ShouldReturnTask() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // When
        Optional<Task> result = taskService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getTitle()).isEqualTo("Test Task");
    }

    @Test
    void findById_WhenTaskNotExists_ShouldReturnEmpty() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.findById(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void markTaskAsInWork_WhenTaskNotStarted_ShouldChangeStatus() {
        // Given
        task.setStatus(Status.НЕ_НАЧАТА);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        String result = taskService.markTaskAsInWork(1L);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        assertThat(task.getStatus()).isEqualTo(Status.В_РАБОТЕ);
        verify(taskRepository).save(task);
    }

    @Test
    void markTaskAsInWork_WhenTaskAlreadyInWork_ShouldReturnError() {
        // Given
        task.setStatus(Status.В_РАБОТЕ);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // When
        String result = taskService.markTaskAsInWork(1L);

        // Then
        assertThat(result).isEqualTo("Так нельзя");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void markTaskAsInWork_WhenTaskNotFound_ShouldThrowException() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskService.markTaskAsInWork(1L))
                .isInstanceOf(NotFoundExeption.class)
                .hasMessage("задача не найдена");
    }

    @Test
    void markTaskAsCompleted_WhenTaskInWork_ShouldChangeStatus() {
        // Given
        task.setStatus(Status.В_РАБОТЕ);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        String result = taskService.markTaskAsCompleted(1L);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        assertThat(task.getStatus()).isEqualTo(Status.ЗАВЕРШЕНА);
        verify(taskRepository).save(task);
        verify(notificationProduser).sendNotificationForAdmin(anyString());
    }

    @Test
    void markTaskAsCompleted_WhenTaskOnRework_ShouldChangeStatus() {
        // Given
        task.setStatus(Status.НА_ДОРАБОТКЕ);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        String result = taskService.markTaskAsCompleted(1L);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        assertThat(task.getStatus()).isEqualTo(Status.ЗАВЕРШЕНА);
        verify(taskRepository).save(task);
        verify(notificationProduser).sendNotificationForAdmin(anyString());
    }

    @Test
    void markTaskAsCompleted_WhenTaskNotStarted_ShouldReturnError() {
        // Given
        task.setStatus(Status.НЕ_НАЧАТА);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // When
        String result = taskService.markTaskAsCompleted(1L);

        // Then
        assertThat(result).isEqualTo("Так нельзя");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void markTaskAsOnRework_WhenTaskCompleted_ShouldChangeStatus() {
        // Given
        task.setStatus(Status.ЗАВЕРШЕНА);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(commentRepositopy.save(any(Comment.class))).thenReturn(comment);

        // When
        String result = taskService.markTaskAsOnRework(1L, comment);

        // Then
        assertThat(result).isEqualTo("Статус изменен");
        assertThat(task.getStatus()).isEqualTo(Status.НА_ДОРАБОТКЕ);
        assertThat(task.getComments()).contains(comment);
        verify(taskRepository).save(task);
        verify(commentRepositopy).save(comment);
        verify(notificationProduser).sendNotificationForUser(anyString(), anyString());
    }

    @Test
    void markTaskAsOnRework_WhenTaskNotCompleted_ShouldReturnError() {
        // Given
        task.setStatus(Status.В_РАБОТЕ);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // When
        String result = taskService.markTaskAsOnRework(1L, comment);

        // Then
        assertThat(result).isEqualTo("Так нельзя");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_WhenTaskExists_ShouldUpdateTask() {
        // Given
        Task updatedTask = Task.builder()
                .title("Test Task")
                .description("Updated Description")
                .status(Status.В_РАБОТЕ)
                .importance(Importance.НАДО_ПОТОРОПИТЬСЯ)
                .deadline(LocalDateTime.now().plusDays(2))
                .build();

        when(taskRepository.findTaskByTitle("Test Task")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        String result = taskService.updateTask(updatedTask);

        // Then
        assertThat(result).isEqualTo("Задача обновлена");
        assertThat(task.getDescription()).isEqualTo("Updated Description");
        assertThat(task.getStatus()).isEqualTo(Status.В_РАБОТЕ);
        assertThat(task.getImportance()).isEqualTo(Importance.НАДО_ПОТОРОПИТЬСЯ);
        verify(taskRepository).save(task);
        verify(notificationProduser).sendNotificationForUser(anyString(), anyString());
    }

    @Test
    void updateTask_WhenTaskNotFound_ShouldThrowException() {
        // Given
        Task updatedTask = Task.builder()
                .title("Non-existent Task")
                .build();

        when(taskRepository.findTaskByTitle("Non-existent Task")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskService.updateTask(updatedTask))
                .isInstanceOf(NotFoundExeption.class)
                .hasMessage("Задача не найдена");
    }
}
