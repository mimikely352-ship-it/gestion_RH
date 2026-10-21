package com.sirh.organisation.repository;

import com.sirh.organisation.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByDepartementId(Long departementId);
}
