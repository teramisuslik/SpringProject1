package com.example.server1.controller;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationProduser {

    String topicForAdmin = "notifications_for_admin";
    String topicForUser = "notifications_for_user";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendNotificationForAdmin(String message){
        kafkaTemplate.send(topicForAdmin, message);
    }

    public void sendNotificationForUser(String message, String username){
        kafkaTemplate.send(topicForUser + username, message);
    }
}
