package com.sirh.organisation.repository;

import com.sirh.organisation.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByServiceId(Long serviceId);
}
