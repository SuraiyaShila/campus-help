package com.bup.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SharedResourceRepository extends JpaRepository<SharedResource, Long> {
    List<SharedResource> findAllByOrderByCreatedAtDesc();
}