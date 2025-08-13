package com.example.server1.service;

import com.example.server1.entity.Role;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.exeptions.NotFoundExeption;
import com.example.server1.repository.TaskRepository;
import com.example.server1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final TaskService taskService;
    private final PasswordEncoder passwordEncoder;
    private final TaskRepository taskRepository;

    public User create(String username, String password) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .tasks(new ArrayList<>())
                .build();
        return userRepository.save(user);
    }

    public User createAdmin(String username, String password) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .tasks(new ArrayList<>())
                .build();
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        return  userRepository.findByUsername(username)
                .filter(e -> passwordEncoder.matches(password, e.getPassword()))
                .orElseThrow(() -> new RuntimeException("при попытке входа что-то пошло не так"));
    }

    public User addTasks(String username, List<Task> taskList) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (taskList != null) {
            for (Task task : taskList) {
                task.setAssignee(user);
                taskRepository.save(task);
                List<Task> tasks = user.getTasks() != null ? user.getTasks() : new ArrayList<>();
                tasks.add(task);
                user.setTasks(tasks);
            }
        }
        else{
            throw new NotFoundExeption("пустой список задач");
        }

        return userRepository.save(user);
    }


    public User findByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.orElseThrow(
                () -> new NotFoundExeption("такого пользователя нет")
        );
    }

    public String getUsername(String username) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new NotFoundExeption("такого пользователя нет")
                );
        return user.getUsername();
    }

    public Role getRole(String username) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new NotFoundExeption("такого пользователя нет")
                );
        return user.getRole();
    }

    @Transactional
    public void deleteUserByUsername(String username){
        userRepository.deleteByUsername(username);
    }
}