package com.example.server1.entity;


public class TaskDeleteDto {
    private String username;
    private Long id;

    // геттеры и сеттеры
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}