package com.example.server1.controller;

import com.example.server1.entity.Role;
import com.example.server1.entity.Task;
import com.example.server1.entity.User;
import com.example.server1.jwt.AuthRequest;
import com.example.server1.jwt.AuthResponse;
import com.example.server1.jwt.JwtTokenUtils;
import com.example.server1.service.TaskService;
import com.example.server1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class Controller {

    private final TaskService taskService;
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;

    @PostMapping("/register")
    public String createUser(@RequestBody AuthRequest request) {
        log.info("register...");
        userService.create(request.getUsername(),request.getPassword());
        log.info("register finished");
        return "redirect:/login";
    }

    @PostMapping("/registeradmin")
    public String createAdmin(@RequestBody AuthRequest request) {
        log.info("register...");
        userService.createAdmin(request.getUsername(),request.getPassword());
        log.info("register finished");
        return "redirect:/login";
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        log.info("login...");
        User user = userService.login(request.getUsername(), request.getPassword());
        String token = jwtTokenUtils.generateToken(user);
        log.info("login finished");
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/main")
    public String getMainPage() {
        return "main";
    }

    @GetMapping("/user")
    public User getUser(@RequestParam String username) {
        log.info("getUser");
        return userService.findByUsername(username);
    }

    @GetMapping("/allusers")
    public List<User> getUsers() {
        return userService.findAll();
    }

    @GetMapping("/userwithouttasks")
    public Map<String,Object> getUserWithoutTasks(@RequestParam String username) {
        log.info("getUser");
        User user = userService.getUserByUsername(username);
        HashMap<String,Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("role", user.getRole());
        return map;
    }

    @PutMapping("/markthetaskascompleted")
    @PreAuthorize("hasRole('ADMIN')")
    public Task markTaskAsCompleted(@RequestParam String title) {
        return taskService.markTaskAsCompleted(title);
    }

    @DeleteMapping("/deleteuser/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable String username) {
        log.info("deleteUser");
        userService.deleteUserByUsername(username);
        log.info("User deleted");
        return ResponseEntity.ok("User deleted");
    }

    @GetMapping("/allusersname")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<String> getAllUsers() {
        List<String> list =  userService.findAllUsername();
        log.info(list.toString());
        return list;
    }


}
