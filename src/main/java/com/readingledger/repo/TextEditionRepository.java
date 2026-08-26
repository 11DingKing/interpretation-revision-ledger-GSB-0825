package com.readingledger.repo;

import com.readingledger.domain.TextEdition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TextEditionRepository extends JpaRepository<TextEdition, UUID> {

    List<TextEdition> findAllByOrderByCreatedAtAscIdAsc();

    boolean existsByTitle(String title);
}
