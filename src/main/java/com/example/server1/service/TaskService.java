package com.example.server1.service;

import com.example.server1.entity.Status;
import com.example.server1.entity.Task;
import com.example.server1.exeptions.NotFoundExeption;
import com.example.server1.repository.TaskRepository;
import com.example.server1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public Task markTaskAsCompleted(String title) {
        Task task = taskRepository.findTaskByTitle(title).orElseThrow(() -> new NotFoundExeption("задача не найдена"));
        task.setStatus(Status.ЗАВЕРШЕНА);
        return taskRepository.save(task);
    }
}
