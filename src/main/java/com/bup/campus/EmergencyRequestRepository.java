package com.bup.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {
    List<EmergencyRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);
    List<EmergencyRequest> findAllByOrderByCreatedAtDesc();
    List<EmergencyRequest> findByTypeOrderByCreatedAtDesc(String type);
}