package com.example.server1;

import com.example.server1.entity.Role;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.exeptions.NotFoundExeption;
import com.example.server1.repository.TaskRepository;
import com.example.server1.repository.UserRepository;
import com.example.server1.service.UserService;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ApplicationTests {

    UserService userService;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    TaskRepository taskRepository;


    @BeforeEach
    public void beforeEach() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder =  Mockito.mock(PasswordEncoder.class);
        taskRepository = Mockito.mock(TaskRepository.class);
        userService = new UserService(userRepository, passwordEncoder, taskRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "Ivan, 123",
            "Alex, 321"
    })
     void givenUsernameandPassword_whenCreate_thanSaveInDb(String username, String password) {
        // Устанавливаем поведение мока passwordEncoder
        String encodedPassword = "encoded_" + password;
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        // Вызываем тестируемый метод
        userService.create(username, password);

        // Проверяем, что save был вызван с ожидаемыми параметрами
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(username, savedUser.getUsername());
        assertEquals(encodedPassword, savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());
        assertNotNull(savedUser.getTasks());
        assertTrue(savedUser.getTasks().isEmpty());
    }


    @ParameterizedTest
    @CsvSource({
            "Ivan",
            "Alex"
    })
    void givenUsername_whenFindByUsername_thenCallRepository(String username) {
        User mockUser = User.builder().username(username).build();

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(mockUser));

        User result = userService.findByUsername(username);

        verify(userRepository).findByUsername(username);
        assertEquals(username, result.getUsername());
    }

    @Test
    void givenUserAndTasks_whenAddTasks_thenAllSavedCorrectly() {
        String username = "testUser";
        User user = User.builder()
                .username(username)
                .password("123")
                .tasks(new ArrayList<>())
                .build();

        Task task1 = Task.builder().title("Task 1").status(false).build();
        Task task2 = Task.builder().title("Task 2").status(true).build();
        List<Task> tasks = List.of(task1, task2);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.addTasks(username, tasks);

        verify(userRepository).findByUsername(username);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(2)).save(taskCaptor.capture());

        assertThat(user.getTasks()).hasSize(tasks.size());
        assertThat(user.getTasks()).containsAll(tasks);
        assertThat(user.getTasks()).allMatch(task -> task.getAssignee() == user);
    }

    @Test
    void givenNonExistentUser_whenAddTasks_thenThrowException() {
        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addTasks("unknown", List.of(new Task())))
                .isInstanceOf(NotFoundExeption.class)
                .hasMessageContaining("пользователь не найден");
    }

    @Test
    void givenNullTaskList_whenAddTasks_thenThrowException() {
        String username = "testUser";
        User user = User.builder().username(username).build();

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.addTasks(username, null))
                .isInstanceOf(NotFoundExeption.class)
                .hasMessageContaining("пустой список задач");
    }


}
