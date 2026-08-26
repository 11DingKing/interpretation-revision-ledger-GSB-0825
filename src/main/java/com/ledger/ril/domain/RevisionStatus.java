package com.ledger.ril.domain;

/** Lifecycle state of a hypothesis revision. Append-only: a state change is a new revision. */
public enum RevisionStatus {
    /** The current, standing interpretation at the head of the chain. */
    ACTIVE,
    /** A prior interpretation that a later revision replaced. */
    SUPERSEDED,
    /** An interpretation the reader explicitly retracted (a WITHDRAWN head). */
    WITHDRAWN
}
