package com.sirh.candidat.repository;

import com.sirh.candidat.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findFirstByCandidatIdAndTypeOrderByDateUploadDesc(Long candidatId, String type);
    java.util.List<Document> findByCandidatIdOrderByDateUploadDesc(Long candidatId);
    java.util.List<Document> findByDossierIdOrderByDateUploadDesc(Long dossierId);
    java.util.List<Document> findByDepartId(Long departId);
    java.util.List<Document> findByDossierIdIsNotNullAndDateExpirationLessThanEqualOrderByDateExpirationAsc(java.time.LocalDate limite);
}
