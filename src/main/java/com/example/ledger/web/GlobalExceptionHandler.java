package com.example.ledger.web;

import com.example.ledger.service.HashMismatchException;
import com.example.ledger.service.HeadConflictException;
import com.example.ledger.service.NotFoundException;
import com.example.ledger.service.StateConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:ledger:not-found"));
        return problem;
    }

    @ExceptionHandler(HeadConflictException.class)
    public ProblemDetail headConflict(HeadConflictException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:ledger:head-conflict"));
        problem.setTitle("Head revision conflict");
        problem.setProperty("currentHeadRevisionId", ex.getCurrentHeadRevisionId());
        return problem;
    }

    @ExceptionHandler(HashMismatchException.class)
    public ProblemDetail hashMismatch(HashMismatchException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("urn:ledger:hash-mismatch"));
        problem.setTitle("Source hash mismatch");
        problem.setProperty("code", "HASH_MISMATCH");
        return problem;
    }

    @ExceptionHandler(StateConflictException.class)
    public ProblemDetail stateConflict(StateConflictException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:ledger:state-conflict"));
        return problem;
    }
}
