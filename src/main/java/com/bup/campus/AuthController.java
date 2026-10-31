package com.bup.campus;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final List<String> ROLES =
        List.of("Student", "Faculty", "Campus Security", "Medical Staff", "Hostel Authority");

    public AuthController(UserRepository repo) {
        this.repo = repo;
    }

    record RegisterRequest(String name, String universityId, String email, String phone,
                           String department, String role, String password) {}

    record LoginRequest(String universityId, String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest r) {
        if (r.name() == null || r.name().isBlank()
                || r.universityId() == null || r.universityId().isBlank()
                || r.password() == null || r.password().length() < 8) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Please fill all required fields. Password needs 8+ characters."));
        }
        if (!ROLES.contains(r.role())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role."));
        }
        if (repo.existsByUniversityId(r.universityId())) {
            return ResponseEntity.status(409)
                .body(Map.of("message", "This university ID is already registered."));
        }
        repo.save(new User(r.name(), r.universityId(), r.email(), r.phone(),
                r.department(), r.role(), encoder.encode(r.password())));
        return ResponseEntity.ok(Map.of("message", "Account created."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest r, HttpServletRequest request) {
        if (r.universityId() == null || r.password() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Wrong ID or password."));
        }
        var found = repo.findByUniversityId(r.universityId());
        if (found.isEmpty() || !encoder.matches(r.password(), found.get().getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("message", "Wrong ID or password."));
        }
        var u = found.get();
        var old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true).setAttribute(Auth.KEY, u.getUniversityId());
        return ResponseEntity.ok(Map.of(
            "name", u.getFullName(), "role", u.getRole(), "universityId", u.getUniversityId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Signed out."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        String id = Auth.id();
        if (id == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Please sign in first."));
        }
        var found = repo.findByUniversityId(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Please sign in first."));
        }
        var u = found.get();
        return ResponseEntity.ok(Map.of(
            "name", u.getFullName(), "role", u.getRole(), "universityId", u.getUniversityId()));
    }
}