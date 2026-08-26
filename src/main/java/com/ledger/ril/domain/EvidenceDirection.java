package com.ledger.ril.domain;

/** Direction in which a piece of evidence bears on a hypothesis. */
public enum EvidenceDirection {
    /** The passage bolsters the hypothesis. */
    SUPPORTS,
    /** The passage cuts against the hypothesis. */
    CHALLENGES,
    /** The passage narrows or conditions the hypothesis without refuting it. */
    QUALIFIES
}
