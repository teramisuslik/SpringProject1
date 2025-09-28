package com.example.server1.service;

import com.example.server1.entity.Comment;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public String markTaskAsInWork(String title) {
        Task task = taskRepository.findTaskByTitle(title).orElseThrow(() -> new NotFoundExeption("задача не найдена"));
        if (task.getStatus() == Status.НЕ_НАЧАТА) {
            task.setStatus(Status.В_РАБОТЕ);
            taskRepository.save(task);
            return "Статус изменен";
        }
        return "Так нельзя";
    }

    public String markTaskAsCompleted(String title) {
        Task task = taskRepository.findTaskByTitle(title).orElseThrow(() -> new NotFoundExeption("задача не найдена"));
        if (task.getStatus() == Status.В_РАБОТЕ || task.getStatus() == Status.НА_ДОРАБОТКЕ) {
            task.setStatus(Status.ЗАВЕРШЕНА);
            taskRepository.save(task);
            return "Статус изменен";
        }
        return "Так нельзя";
    }

    public String markTaskAsOnRework(String title, Comment comment) {
        Task task = taskRepository.findTaskByTitle(title).orElseThrow(() -> new NotFoundExeption("задача не найдена"));
        if (task.getStatus() == Status.ЗАВЕРШЕНА) {
            List<Comment> comments = task.getComments() != null ? task.getComments() : new ArrayList<>();
            comments.add(comment);
            task.setComments(comments);
            task.setStatus(Status.НА_ДОРАБОТКЕ);
            return "Статус изменен";
        }
        return "Так нельзя";
    }
}
