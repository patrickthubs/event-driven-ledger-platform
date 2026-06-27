package za.co.patrick.ledgerplatform.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import za.co.patrick.ledgerplatform.application.ResourceNotFoundException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleBadRequest(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid ledger request", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid.");
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (left, right) -> left));
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleConflict(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "Data integrity violation", "The request conflicts with an existing record.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://patrick.co.za/problems/%s".formatted(title.toLowerCase().replace(' ', '-'))));
        return problemDetail;
    }
}
