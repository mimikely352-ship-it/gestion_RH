package com.sirh.candidat.repository;

import com.sirh.candidat.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findFirstByCandidatIdAndTypeOrderByDateUploadDesc(Long candidatId, String type);
}
