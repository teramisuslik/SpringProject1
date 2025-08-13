package com.example.server1.jwt;

import com.example.server1.entity.Role;
import com.example.server1.entity.Task;
import lombok.Data;

import java.util.List;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
