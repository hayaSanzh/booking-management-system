package com.booking.repository;

import com.booking.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {
    
    Page<Resource> findByIsActiveTrue(Pageable pageable);

    java.util.Optional<Resource> findByIdAndIsActiveTrue(Long id);
}
