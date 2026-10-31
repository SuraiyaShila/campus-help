package com.bup.campus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Map<String, String> ROLE_TYPE =
        Map.of("Medical Staff", "Medical", "Campus Security", "Security", "Hostel Authority", "Hostel");

    private final UserRepository users;
    private final EmergencyRequestRepository emergencies;
    private final LostFoundItemRepository items;
    private final ClaimRequestRepository claims;
    private final ResourceRequestRepository resourceRequests;

    public NotificationController(UserRepository users, EmergencyRequestRepository emergencies,
                                  LostFoundItemRepository items, ClaimRequestRepository claims,
                                  ResourceRequestRepository resourceRequests) {
        this.users = users;
        this.emergencies = emergencies;
        this.items = items;
        this.claims = claims;
        this.resourceRequests = resourceRequests;
    }

    private record Note(long time, String type, String message) {}

    private static long ms(LocalDateTime t) {
        return t == null ? 0 : t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static String label(String status) {
        return "InProgress".equals(status) ? "in progress" : status.toLowerCase();
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String ignored) {
        String universityId = Auth.id();
        var user = users.findByUniversityId(universityId);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Please sign in first."));
        }
        String me = user.get().getUniversityId();
        String role = user.get().getRole();
        boolean admin = "Administrator".equals(role);
        boolean reviewer = admin || "Campus Security".equals(role);
        List<Note> notes = new ArrayList<>();

        // Emergency: status changes on my own requests
        for (EmergencyRequest r : emergencies.findByRequesterIdOrderByCreatedAtDesc(me)) {
            if (!"Pending".equals(r.getStatus())) {
                notes.add(new Note(ms(r.getUpdatedAt()), "emergency",
                    "Your " + r.getType() + " request is now " + label(r.getStatus()) + "."));
            }
        }
        // Emergency: new requests waiting for my team
        List<EmergencyRequest> forMe;
        if (admin) {
            forMe = emergencies.findAllByOrderByCreatedAtDesc();
        } else if (ROLE_TYPE.containsKey(role)) {
            forMe = emergencies.findByTypeOrderByCreatedAtDesc(ROLE_TYPE.get(role));
        } else {
            forMe = List.of();
        }
        for (EmergencyRequest r : forMe) {
            if ("Pending".equals(r.getStatus())) {
                notes.add(new Note(ms(r.getCreatedAt()), "emergency",
                    "New " + r.getUrgency().toLowerCase() + " urgency " + r.getType()
                        + " request from " + r.getRequesterName() + " at " + r.getLocation() + "."));
            }
        }

        // Lost and found
        Set<Long> myItems = items.findByReporterId(me).stream()
            .map(LostFoundItem::getId).collect(Collectors.toSet());
        for (ClaimRequest c : claims.findByStatusOrderByCreatedAtDesc("Pending")) {
            if (myItems.contains(c.getItemId())) {
                notes.add(new Note(ms(c.getCreatedAt()), "lostfound",
                    c.getClaimerName() + " sent a claim for your item \"" + c.getItemName() + "\"."));
            }
            if (reviewer) {
                notes.add(new Note(ms(c.getCreatedAt()), "lostfound",
                    "New claim from " + c.getClaimerName() + " for \"" + c.getItemName() + "\" needs review."));
            }
        }
        for (ClaimRequest c : claims.findByClaimerIdOrderByCreatedAtDesc(me)) {
            if (!"Pending".equals(c.getStatus())) {
                notes.add(new Note(ms(c.getUpdatedAt()), "lostfound",
                    "Your claim for \"" + c.getItemName() + "\" was " + c.getStatus().toLowerCase() + "."));
            }
        }

        // Resource sharing
        for (ResourceRequest r : resourceRequests.findByOwnerIdAndStatusInOrderByCreatedAtDesc(me, List.of("Pending"))) {
            notes.add(new Note(ms(r.getCreatedAt()), "resource",
                r.getRequesterName() + " requested your resource \"" + r.getResourceName() + "\"."));
        }
        for (ResourceRequest r : resourceRequests.findByRequesterIdOrderByCreatedAtDesc(me)) {
            if (!"Pending".equals(r.getStatus())) {
                notes.add(new Note(ms(r.getUpdatedAt()), "resource",
                    "Your request for \"" + r.getResourceName() + "\" was " + r.getStatus().toLowerCase() + "."));
            }
        }

        var result = notes.stream()
            .sorted(Comparator.comparingLong(Note::time).reversed())
            .limit(30)
            .map(n -> Map.<String, Object>of("time", n.time(), "type", n.type(), "message", n.message()))
            .toList();
        return ResponseEntity.ok(result);
    }
}