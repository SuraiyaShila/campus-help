package com.bup.campus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String[] SLOTS = {"12-4 AM", "4-8 AM", "8-12 AM", "12-4 PM", "4-8 PM", "8-12 PM"};

    private final UserRepository users;
    private final EmergencyRequestRepository emergencies;
    private final LostFoundItemRepository items;
    private final SharedResourceRepository resources;

    public AdminController(UserRepository users, EmergencyRequestRepository emergencies,
                           LostFoundItemRepository items, SharedResourceRepository resources) {
        this.users = users;
        this.emergencies = emergencies;
        this.items = items;
        this.resources = resources;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        var user = users.findByUniversityId(universityId);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Please sign in first."));
        }
        if (!"Administrator".equals(user.get().getRole())) {
            return ResponseEntity.status(403).body(Map.of("message", "Only administrators can see this."));
        }

        List<EmergencyRequest> all = emergencies.findAll();
        long active = all.stream()
            .filter(e -> !"Resolved".equals(e.getStatus()) && !"Rejected".equals(e.getStatus()))
            .count();
        List<LostFoundItem> allItems = items.findAll();
        long lostOpen = allItems.stream()
            .filter(i -> "Lost".equals(i.getType()) && "Open".equals(i.getStatus()))
            .count();
        long lent = resources.findAll().stream()
            .filter(x -> "Lent".equals(x.getStatus()))
            .count();

        int[] buckets = new int[6];
        for (EmergencyRequest e : all) {
            buckets[e.getCreatedAt().getHour() / 4]++;
        }
        List<Map<String, Object>> peak = new ArrayList<>();
        for (int k = 0; k < SLOTS.length; k++) {
            peak.add(Map.<String, Object>of("label", SLOTS[k], "count", buckets[k]));
        }

        Map<String, Long> catCounts = allItems.stream().collect(
            Collectors.groupingBy(LostFoundItem::getCategory, TreeMap::new, Collectors.counting()));
        List<Map<String, Object>> categories = new ArrayList<>();
        catCounts.forEach((name, count) -> categories.add(Map.<String, Object>of("category", name, "count", count)));

        List<Map<String, Object>> userList = users.findAll().stream()
            .map(u -> Map.<String, Object>of("name", u.getFullName(), "universityId", u.getUniversityId(), "role", u.getRole()))
            .toList();

        return ResponseEntity.ok(Map.<String, Object>of(
            "totalCases", all.size(),
            "activeCases", active,
            "lostItems", lostOpen,
            "resourcesLent", lent,
            "peak", peak,
            "categories", categories,
            "users", userList));
    }
}