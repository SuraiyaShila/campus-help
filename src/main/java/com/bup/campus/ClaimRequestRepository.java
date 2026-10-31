package com.bup.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimRequestRepository extends JpaRepository<ClaimRequest, Long> {
    List<ClaimRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<ClaimRequest> findByItemIdAndStatus(Long itemId, String status);
    List<ClaimRequest> findByClaimerIdOrderByCreatedAtDesc(String claimerId);
    boolean existsByItemIdAndClaimerIdAndStatus(Long itemId, String claimerId, String status);
}