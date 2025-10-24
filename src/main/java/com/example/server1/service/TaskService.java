package com.example.server1.service;

import com.example.server1.controller.NotificationProduser;
import com.example.server1.entity.Comment;
import com.example.server1.entity.Status;
import com.example.server1.entity.Task;
import com.example.server1.exeptions.NotFoundExeption;
import com.example.server1.repository.CommentRepositopy;
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
    private final CommentRepositopy commentRepositopy;
    private final NotificationProduser notificationProduser;

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public String markTaskAsInWork(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new NotFoundExeption("задача не найдена"));
        if (task.getStatus() == Status.НЕ_НАЧАТА) {
            task.setStatus(Status.В_РАБОТЕ);
            taskRepository.save(task);
            return "Статус изменен";
        }
        return "Так нельзя";
    }

    public String markTaskAsCompleted(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new NotFoundExeption("задача не найдена"));
        if (task.getStatus() == Status.В_РАБОТЕ || task.getStatus() == Status.НА_ДОРАБОТКЕ) {
            task.setStatus(Status.ЗАВЕРШЕНА);
            notificationProduser.sendNotificationForAdmin("пользователь " + task.getAssignee().getUsername() + " завершил задачу " + task.getTitle());
            taskRepository.save(task);
            return "Статус изменен";
        }
        return "Так нельзя";
    }

    public String markTaskAsOnRework(Long id, Comment comment) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new NotFoundExeption("задача не найдена"));
        if (task.getStatus() == Status.ЗАВЕРШЕНА) {
            List<Comment> comments = task.getComments() != null ? task.getComments() : new ArrayList<>();
            comments.add(comment);
            task.setComments(comments);
            task.setStatus(Status.НА_ДОРАБОТКЕ);
            taskRepository.save(task);
            comment.setTask(task);
            commentRepositopy.save(comment);

            notificationProduser.sendNotificationForUser("задачу " + task.getTitle() + " отправили на доработку", task.getAssignee().getUsername());
            return "Статус изменен";
        }
        return "Так нельзя";
    }

    public String updateTask(Task task) {
        Task existingTask = taskRepository.findTaskByTitle(task.getTitle())
                .orElseThrow(() -> new NotFoundExeption("Задача не найдена"));

        if (task.getDescription() != null) {
            existingTask.setDescription(task.getDescription());
        }
        if (task.getStatus() != null) {
            existingTask.setStatus(task.getStatus());
        }
        if (task.getImportance() != null) {
            existingTask.setImportance(task.getImportance());
        }
        if (task.getDeadline() != null) {
            existingTask.setDeadline(task.getDeadline());
        }

        taskRepository.save(existingTask);
        notificationProduser.sendNotificationForUser("задача " + task.getTitle() + " изменена", existingTask.getAssignee().getUsername());

        return "Задача обновлена";
    }
}
