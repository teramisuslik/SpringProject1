package com.example.server1.controller;


import com.example.server1.entity.Importance;
import com.example.server1.entity.Status;
import com.example.server1.entity.TaskDto;
import com.example.server1.entity.Task;
import com.example.server1.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskConsumer {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "task-assignments")
    @Transactional
    public void consumeTask(String taskJson) {
        try {
            log.info("Получено сообщение из Kafka: {}", taskJson);

            // Парсим в DTO
            TaskDto taskDto = objectMapper.readValue(taskJson, TaskDto.class);

            // Конвертируем DTO в Entity
            Task task = convertToEntity(taskDto);

            // Сохраняем через ваш сервис
            userService.addTasks(taskDto.getAssignedUser(), task);

            log.info("Задача сохранена для пользователя: {}", taskDto.getAssignedUser());

        } catch (Exception e) {
            log.error("Ошибка обработки задачи из Kafka: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private Task convertToEntity(TaskDto dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());

        // Конвертируем строку в Enum Importance
        task.setImportance(convertToImportance(dto.getImportance()));

        // Конвертируем строку в Enum Status
        task.setStatus(convertToStatus(dto.getStatus()));

        // Конвертируем строку в LocalDateTime
        if (dto.getDeadline() != null) {
            try {
                LocalDateTime deadline = parseDateTime(dto.getDeadline());
                task.setDeadline(deadline);
            } catch (Exception e) {
                log.error("Ошибка парсинга даты: {}", dto.getDeadline());
                task.setDeadline(LocalDateTime.now().plusDays(1));
            }
        } else {
            task.setDeadline(LocalDateTime.now().plusDays(1));
        }

        return task;
    }

    private Importance convertToImportance(String importanceStr) {
        if (importanceStr == null) {
            return Importance.МОЖЕТ_ПОДОЖДАТЬ; // значение по умолчанию
        }

        try {
            return Importance.valueOf(importanceStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Неизвестный тип важности: {}, используем значение по умолчанию", importanceStr);
            return Importance.МОЖЕТ_ПОДОЖДАТЬ;
        }
    }

    private Status convertToStatus(String statusStr) {
        if (statusStr == null) {
            return Status.НЕ_НАЧАТА; // значение по умолчанию
        }

        try {
            return Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Неизвестный статус: {}, используем значение по умолчанию", statusStr);
            return Status.НЕ_НАЧАТА;
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (Exception e) {
                continue;
            }
        }
        throw new IllegalArgumentException("Неизвестный формат даты: " + dateTimeStr);
    }
}