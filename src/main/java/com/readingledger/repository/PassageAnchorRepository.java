package com.readingledger.repository;

import com.readingledger.domain.PassageAnchor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PassageAnchorRepository extends JpaRepository<PassageAnchor, UUID> {

    List<PassageAnchor> findByEditionIdOrderByCreatedAtAscIdAsc(UUID editionId);
}
