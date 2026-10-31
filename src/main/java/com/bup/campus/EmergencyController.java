package com.bup.campus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    private static final List<String> TYPES = List.of("Medical", "Security", "Hostel");
    private static final List<String> URGENCY = List.of("Low", "Medium", "High");
    private static final List<String> STATUSES =
        List.of("Pending", "Accepted", "InProgress", "Resolved", "Rejected");
    // Each responder role handles one request type. Administrator sees everything.
    private static final Map<String, String> ROLE_TYPE =
        Map.of("Medical Staff", "Medical", "Campus Security", "Security", "Hostel Authority", "Hostel");

    private final EmergencyRequestRepository repo;
    private final UserRepository users;

    public EmergencyController(EmergencyRequestRepository repo, UserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    record CreateRequest(String universityId, String type, String location,
                         String urgency, String description) {}

    record StatusRequest(String universityId, String status) {}

    private static Map<String, Object> toMap(EmergencyRequest r) {
        return Map.<String, Object>of(
            "id", r.getId(),
            "requesterId", r.getRequesterId(),
            "requesterName", r.getRequesterName(),
            "type", r.getType(),
            "location", r.getLocation(),
            "urgency", r.getUrgency(),
            "description", r.getDescription(),
            "status", r.getStatus(),
            "createdAt", String.valueOf(r.getCreatedAt()),
            "updatedAt", String.valueOf(r.getUpdatedAt()));
    }

    private static ResponseEntity<?> error(int code, String message) {
        return ResponseEntity.status(code).body(Map.of("message", message));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest r) {
        var user = users.findByUniversityId(Auth.id());
        if (user.isEmpty()) return error(401, "Please sign in first.");
        if (!TYPES.contains(r.type()) || !URGENCY.contains(r.urgency())
                || r.location() == null || r.location().isBlank()
                || r.description() == null || r.description().isBlank()) {
            return error(400, "Please fill in type, location, urgency and description.");
        }
        var saved = repo.save(new EmergencyRequest(
            user.get().getUniversityId(), user.get().getFullName(),
            r.type(), r.location().trim(), r.urgency(), r.description().trim()));
        return ResponseEntity.ok(toMap(saved));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        if (users.findByUniversityId(universityId).isEmpty()) return error(401, "Please sign in first.");
        var list = repo.findByRequesterIdOrderByCreatedAtDesc(universityId)
            .stream().map(EmergencyController::toMap).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<?> all(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        var user = users.findByUniversityId(universityId);
        if (user.isEmpty()) return error(401, "Please sign in first.");
        String role = user.get().getRole();
        List<EmergencyRequest> list;
        if ("Administrator".equals(role)) {
            list = repo.findAllByOrderByCreatedAtDesc();
        } else if (ROLE_TYPE.containsKey(role)) {
            list = repo.findByTypeOrderByCreatedAtDesc(ROLE_TYPE.get(role));
        } else {
            return error(403, "You are not allowed to see all requests.");
        }
        return ResponseEntity.ok(list.stream().map(EmergencyController::toMap).toList());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusRequest s) {
        var user = users.findByUniversityId(Auth.id());
        if (user.isEmpty()) return error(401, "Please sign in first.");
        String role = user.get().getRole();
        boolean admin = "Administrator".equals(role);
        if (!admin && !ROLE_TYPE.containsKey(role)) return error(403, "You are not allowed to change status.");
        if (!STATUSES.contains(s.status())) return error(400, "Invalid status.");
        var found = repo.findById(id);
        if (found.isEmpty()) return error(404, "Request not found.");
        var req = found.get();
        if (!admin && !ROLE_TYPE.get(role).equals(req.getType())) {
            return error(403, "This request belongs to another team.");
        }
        req.setStatus(s.status());
        repo.save(req);
        return ResponseEntity.ok(toMap(req));
    }
}