package com.example.ledger.repo;

import com.example.ledger.domain.TextEdition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TextEditionRepository extends JpaRepository<TextEdition, UUID> {
}
