package com.bup.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LostFoundItemRepository extends JpaRepository<LostFoundItem, Long> {
    List<LostFoundItem> findByStatusOrderByCreatedAtDesc(String status);
    List<LostFoundItem> findByReporterId(String reporterId);
}