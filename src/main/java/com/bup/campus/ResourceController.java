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
@RequestMapping("/api/resources")
public class ResourceController {

    private static final List<String> OPEN = List.of("Pending", "Accepted");

    private final SharedResourceRepository resources;
    private final ResourceRequestRepository requests;
    private final UserRepository users;

    public ResourceController(SharedResourceRepository resources, ResourceRequestRepository requests,
                              UserRepository users) {
        this.resources = resources;
        this.requests = requests;
        this.users = users;
    }

    record ResourceBody(String universityId, String name, String place) {}
    record RequestBody2(String universityId, String message) {}
    record StatusBody(String universityId, String status) {}

    private static Map<String, Object> resourceMap(SharedResource r) {
        return Map.<String, Object>of(
            "id", r.getId(),
            "name", r.getName(),
            "place", r.getPlace(),
            "ownerId", r.getOwnerId(),
            "ownerName", r.getOwnerName(),
            "status", r.getStatus(),
            "createdAt", String.valueOf(r.getCreatedAt()));
    }

    private static Map<String, Object> requestMap(ResourceRequest r) {
        return Map.<String, Object>of(
            "id", r.getId(),
            "resourceId", r.getResourceId(),
            "resourceName", r.getResourceName(),
            "ownerId", r.getOwnerId(),
            "ownerName", r.getOwnerName(),
            "requesterId", r.getRequesterId(),
            "requesterName", r.getRequesterName(),
            "message", r.getMessage(),
            "status", r.getStatus(),
            "createdAt", String.valueOf(r.getCreatedAt()));
    }

    private static ResponseEntity<?> error(int code, String message) {
        return ResponseEntity.status(code).body(Map.of("message", message));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ResourceBody b) {
        var user = users.findByUniversityId(Auth.id());
        if (user.isEmpty()) return error(401, "Please sign in first.");
        if (b.name() == null || b.name().isBlank()) return error(400, "Please enter the resource name.");
        String place = (b.place() == null || b.place().isBlank()) ? "Not specified" : b.place().trim();
        var saved = resources.save(new SharedResource(b.name().trim(), place,
            user.get().getUniversityId(), user.get().getFullName()));
        return ResponseEntity.ok(resourceMap(saved));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        if (users.findByUniversityId(universityId).isEmpty()) return error(401, "Please sign in first.");
        var list = resources.findAllByOrderByCreatedAtDesc()
            .stream().map(ResourceController::resourceMap).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/request")
    public ResponseEntity<?> request(@PathVariable Long id, @RequestBody RequestBody2 b) {
        var user = users.findByUniversityId(Auth.id());
        if (user.isEmpty()) return error(401, "Please sign in first.");
        var found = resources.findById(id);
        if (found.isEmpty() || !"Available".equals(found.get().getStatus())) {
            return error(404, "This resource is not available right now.");
        }
        var res = found.get();
        String me = user.get().getUniversityId();
        if (res.getOwnerId().equals(me)) return error(400, "You cannot request your own resource.");
        if (requests.existsByResourceIdAndRequesterIdAndStatusIn(id, me, OPEN)) {
            return error(409, "You already requested this resource.");
        }
        String message = b.message() == null ? "" : b.message().trim();
        requests.save(new ResourceRequest(res.getId(), res.getName(), res.getOwnerId(), res.getOwnerName(),
            me, user.get().getFullName(), message));
        return ResponseEntity.ok(Map.of("message", "Request sent."));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<?> incoming(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        if (users.findByUniversityId(universityId).isEmpty()) return error(401, "Please sign in first.");
        var list = requests.findByOwnerIdAndStatusInOrderByCreatedAtDesc(universityId, OPEN)
            .stream().map(ResourceController::requestMap).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/requests/mine")
    public ResponseEntity<?> mine(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        if (users.findByUniversityId(universityId).isEmpty()) return error(401, "Please sign in first.");
        var list = requests.findByRequesterIdOrderByCreatedAtDesc(universityId)
            .stream().map(ResourceController::requestMap).toList();
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/requests/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody StatusBody b) {
        var user = users.findByUniversityId(Auth.id());
        if (user.isEmpty()) return error(401, "Please sign in first.");
        var found = requests.findById(id);
        if (found.isEmpty()) return error(404, "Request not found.");
        var req = found.get();
        if (!req.getOwnerId().equals(user.get().getUniversityId())) {
            return error(403, "Only the owner can do this.");
        }
        var resFound = resources.findById(req.getResourceId());
        if (resFound.isEmpty()) return error(404, "Resource not found.");
        var res = resFound.get();
        String status = b.status();

        if ("Accepted".equals(status)) {
            if (!"Pending".equals(req.getStatus())) return error(409, "This request is not pending.");
            if (!"Available".equals(res.getStatus())) return error(409, "This resource is already lent out.");
            req.setStatus("Accepted");
            res.setStatus("Lent");
            for (ResourceRequest other : requests.findByResourceIdAndStatus(res.getId(), "Pending")) {
                if (!other.getId().equals(req.getId())) {
                    other.setStatus("Rejected");
                    requests.save(other);
                }
            }
        } else if ("Rejected".equals(status)) {
            if (!"Pending".equals(req.getStatus())) return error(409, "This request is not pending.");
            req.setStatus("Rejected");
        } else if ("Returned".equals(status)) {
            if (!"Accepted".equals(req.getStatus())) return error(409, "This item is not lent out.");
            req.setStatus("Returned");
            res.setStatus("Available");
        } else {
            return error(400, "Invalid status.");
        }
        requests.save(req);
        resources.save(res);
        return ResponseEntity.ok(requestMap(req));
    }
}