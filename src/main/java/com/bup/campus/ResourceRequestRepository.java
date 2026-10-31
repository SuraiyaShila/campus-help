package com.bup.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceRequestRepository extends JpaRepository<ResourceRequest, Long> {
    List<ResourceRequest> findByOwnerIdAndStatusInOrderByCreatedAtDesc(String ownerId, List<String> statuses);
    List<ResourceRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);
    List<ResourceRequest> findByResourceIdAndStatus(Long resourceId, String status);
    boolean existsByResourceIdAndRequesterIdAndStatusIn(Long resourceId, String requesterId, List<String> statuses);
}