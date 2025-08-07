package br.com.admissao.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleWebExchangeBind(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<String> errors = ex.getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .messages(errors)
                .path(path(exchange))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgNotValid(MethodArgumentNotValidException ex, ServerWebExchange exchange) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .messages(errors)
                .path(path(exchange))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleServerWebInput(ServerWebInputException ex, ServerWebExchange exchange) {
        String msg = extractMessageFromServerWebInput(ex);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Malformed Request")
                .messages(List.of(msg))
                .path(path(exchange))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DecodingException.class)
    public ResponseEntity<ErrorResponse> handleDecoding(DecodingException ex, ServerWebExchange exchange) {
        String msg = "Corpo da requisição inválido (JSON malformado).";
        if (ex.getMessage() != null) msg = msg + " " + ex.getMessage();

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Malformed Request")
                .messages(List.of(msg))
                .path(path(exchange))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .messages(errors)
                .path(path(exchange))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, ServerWebExchange exchange) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("API Error")
                .messages(List.of(ex.getMessage()))
                .path(path(exchange))
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, ServerWebExchange exchange) {
        // log the exception here in real app
        String userMsg = "Erro interno: " + Optional.ofNullable(ex.getMessage()).orElse(ex.getClass().getSimpleName());
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .messages(List.of(userMsg))
                .path(path(exchange))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ---------- helpers ----------

    private String extractMessageFromServerWebInput(ServerWebInputException ex) {
        // percorre a cadeia de causes buscando InvalidFormatException / MismatchedInputException
        Throwable t = ex;
        while (t != null) {
            if (t instanceof InvalidFormatException) {
                InvalidFormatException ife = (InvalidFormatException) t;
                // Se for LocalDate
                if (ife.getTargetType() != null && ife.getTargetType().getSimpleName().contains("LocalDate")) {
                    // tenta identificar o nome do campo (path)
                    String fieldName = guessFieldNameFrom(ife);
                    return String.format("%s: formato inválido. Use yyyy-MM-dd", fieldName != null ? fieldName : "data");
                }
                // generic message for invalid value
                String field = guessFieldNameFrom(ife);
                return "Valor inválido para o campo" + (field != null ? " " + field : "") + ".";
            }
            if (t instanceof MismatchedInputException) {
                MismatchedInputException mie = (MismatchedInputException) t;
                String field = guessFieldNameFrom(mie);
                return "Tipo de dado inválido" + (field != null ? " para o campo " + field : "") + ". Verifique o JSON.";
            }
            t = t.getCause();
        }

        // mensagens de fallback mais informativas
        String msg = ex.getMessage();
        if (msg != null && msg.contains("Failed to read HTTP message")) {
            return "Corpo da requisição inválido ou mal formado (JSON). Verifique campos e formato.";
        }
        if (ex.getReason() != null && !ex.getReason().isBlank()) {
            return ex.getReason();
        }
        return Optional.ofNullable(msg).orElse("Requisição inválida.");
    }

    private String guessFieldNameFrom(InvalidFormatException ife) {
        try {
            if (ife.getPath() != null && !ife.getPath().isEmpty()) {
                return ife.getPath().get(0).getFieldName();
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String guessFieldNameFrom(MismatchedInputException mie) {
        try {
            if (mie.getPath() != null && !mie.getPath().isEmpty()) {
                return mie.getPath().get(0).getFieldName();
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String formatFieldError(FieldError f) {
        return f.getField() + ": " + f.getDefaultMessage();
    }

    private String formatConstraintViolation(ConstraintViolation<?> v) {
        String prop = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "param";
        return prop + ": " + v.getMessage();
    }

    private String path(ServerWebExchange exchange) {
        return exchange != null ? exchange.getRequest().getPath().value() : null;
    }
}
