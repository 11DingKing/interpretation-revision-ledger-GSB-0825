package com.ledger.ril.repo;

import java.util.List;

import com.ledger.ril.domain.PassageAnchor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassageAnchorRepository extends JpaRepository<PassageAnchor, String> {

    List<PassageAnchor> findByEditionIdOrderByCreatedAtAscIdAsc(String editionId);
}
