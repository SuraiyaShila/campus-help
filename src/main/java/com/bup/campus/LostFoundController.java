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
@RequestMapping("/api/lostfound")
public class LostFoundController {

    private static final List<String> TYPES = List.of("Lost", "Found");
    private static final List<String> CATEGORIES =
        List.of("Electronics", "Stationery", "ID & Cards", "Other");
    private static final List<String> REVIEWERS = List.of("Campus Security", "Administrator");
    private static final int MAX_IMAGE_LENGTH = 900_000;

    private final LostFoundItemRepository items;
    private final ClaimRequestRepository claims;
    private final UserRepository users;

    public LostFoundController(LostFoundItemRepository items, ClaimRequestRepository claims,
                               UserRepository users) {
        this.items = items;
        this.claims = claims;
        this.users = users;
    }

    record ItemRequest(String universityId, String type, String name, String category,
                       String place, String image) {}
    record ClaimBody(String universityId, String note) {}
    record ReviewBody(String universityId, String status) {}

    private static Map<String, Object> itemMap(LostFoundItem i) {
        return Map.<String, Object>of(
            "id", i.getId(),
            "type", i.getType(),
            "name", i.getName(),
            "category", i.getCategory(),
            "place", i.getPlace(),
            "reporterId", i.getReporterId(),
            "reporterName", i.getReporterName(),
            "status", i.getStatus(),
            "createdAt", String.valueOf(i.getCreatedAt()),
            "image", i.getImage() == null ? "" : i.getImage());
    }

    private static Map<String, Object> claimMap(ClaimRequest c) {
        return Map.<String, Object>of(
            "id", c.getId(),
            "itemId", c.getItemId(),
            "itemName", c.getItemName(),
            "itemType", c.getItemType(),
            "claimerId", c.getClaimerId(),
            "claimerName", c.getClaimerName(),
            "note", c.getNote(),
            "status", c.getStatus(),
            "createdAt", String.valueOf(c.getCreatedAt()));
    }

    private static ResponseEntity<?> error(int code, String message) {
        return ResponseEntity.status(code).body(Map.of("message", message));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ItemRequest r) {
        var user = users.findByUniversityId(Auth.id());
        if (user.isEmpty()) return error(401, "Please sign in first.");
        if (!TYPES.contains(r.type()) || !CATEGORIES.contains(r.category())
                || r.name() == null || r.name().isBlank()) {
            return error(400, "Please enter the item name, type and category.");
        }
        boolean hasImage = r.image() != null && !r.image().isBlank();
        if (hasImage && (!r.image().startsWith("data:image/") || r.image().length() > MAX_IMAGE_LENGTH)) {
            return error(400, "The photo is invalid or too large.");
        }
        String place = (r.place() == null || r.place().isBlank()) ? "Not specified" : r.place().trim();
        var item = new LostFoundItem(r.type(), r.name().trim(), r.category(), place,
            user.get().getUniversityId(), user.get().getFullName());
        if (hasImage) item.setImage(r.image());
        var saved = items.save(item);
        return ResponseEntity.ok(itemMap(saved));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        if (users.findByUniversityId(universityId).isEmpty()) return error(401, "Please sign in first.");
        var list = items.findByStatusOrderByCreatedAtDesc("Open")
            .stream().map(LostFoundController::itemMap).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<?> claim(@PathVariable Long id, @RequestBody ClaimBody b) {
        var user = users.findByUniversityId(Auth.id());
        if (user.isEmpty()) return error(401, "Please sign in first.");
        if (b.note() == null || b.note().isBlank()) return error(400, "Please write a short note.");
        var found = items.findById(id);
        if (found.isEmpty() || !"Open".equals(found.get().getStatus())) {
            return error(404, "This item is no longer open.");
        }
        var item = found.get();
        if (item.getReporterId().equals(user.get().getUniversityId())) {
            return error(400, "You cannot claim your own report.");
        }
        if (claims.existsByItemIdAndClaimerIdAndStatus(id, user.get().getUniversityId(), "Pending")) {
            return error(409, "You already sent a claim for this item.");
        }
        claims.save(new ClaimRequest(item.getId(), item.getName(), item.getType(),
            user.get().getUniversityId(), user.get().getFullName(), b.note().trim()));
        return ResponseEntity.ok(Map.of("message", "Claim sent."));
    }

    @GetMapping("/claims")
    public ResponseEntity<?> pending(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        var user = users.findByUniversityId(universityId);
        if (user.isEmpty()) return error(401, "Please sign in first.");
        if (!REVIEWERS.contains(user.get().getRole())) return error(403, "Only Security can review claims.");
        var list = claims.findByStatusOrderByCreatedAtDesc("Pending")
            .stream().map(LostFoundController::claimMap).toList();
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/claims/{id}")
    public ResponseEntity<?> review(@PathVariable Long id, @RequestBody ReviewBody b) {
        var user = users.findByUniversityId(Auth.id());
        if (user.isEmpty()) return error(401, "Please sign in first.");
        if (!REVIEWERS.contains(user.get().getRole())) return error(403, "Only Security can review claims.");
        if (!"Approved".equals(b.status()) && !"Rejected".equals(b.status())) {
            return error(400, "Invalid status.");
        }
        var found = claims.findById(id);
        if (found.isEmpty()) return error(404, "Claim not found.");
        var claim = found.get();
        if (!"Pending".equals(claim.getStatus())) return error(409, "This claim was already reviewed.");
        claim.setStatus(b.status());
        claims.save(claim);
        if ("Approved".equals(b.status())) {
            items.findById(claim.getItemId()).ifPresent(item -> {
                item.setStatus("Resolved");
                items.save(item);
            });
            for (ClaimRequest other : claims.findByItemIdAndStatus(claim.getItemId(), "Pending")) {
                other.setStatus("Rejected");
                claims.save(other);
            }
        }
        return ResponseEntity.ok(claimMap(claim));
    }
}