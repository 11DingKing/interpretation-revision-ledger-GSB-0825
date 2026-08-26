package com.example.ledger.repo;

import com.example.ledger.domain.PassageAnchor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PassageAnchorRepository extends JpaRepository<PassageAnchor, UUID> {

    List<PassageAnchor> findByEditionIdOrderByCreatedAtAscIdAsc(UUID editionId);
}
