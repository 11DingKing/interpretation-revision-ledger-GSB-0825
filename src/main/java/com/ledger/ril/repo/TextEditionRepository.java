package com.ledger.ril.repo;

import com.ledger.ril.domain.TextEdition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextEditionRepository extends JpaRepository<TextEdition, String> {

    boolean existsByTitleAndEditorLabel(String title, String editorLabel);
}
