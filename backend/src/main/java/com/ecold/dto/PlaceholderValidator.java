package com.ecold.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderValidator implements ConstraintValidator<ValidPlaceholders, String> {
    
    private static final Set<String> VALID_PLACEHOLDERS = Set.of(
        "Company", "Role", "RecruiterName", "MyName"
    );
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    @Override
    public void initialize(ValidPlaceholders constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotBlank handle null validation
        }
        
        Set<String> invalidPlaceholders = new HashSet<>();
        Set<String> malformedPlaceholders = new HashSet<>();
        
        // Check for malformed placeholders (missing braces)
        if (value.contains("{") || value.contains("}")) {
            // Check for unmatched braces
            checkForMalformedBraces(value, malformedPlaceholders);
        }
        
        // Find all valid placeholder patterns
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!VALID_PLACEHOLDERS.contains(placeholder)) {
                invalidPlaceholders.add(placeholder);
            }
        }
        
        boolean isValid = invalidPlaceholders.isEmpty() && malformedPlaceholders.isEmpty();
        
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            
            if (!invalidPlaceholders.isEmpty()) {
                context.buildConstraintViolationWithTemplate(
                    "Invalid placeholders found: " + String.join(", ", invalidPlaceholders) + 
                    ". Valid placeholders are: " + String.join(", ", VALID_PLACEHOLDERS))
                    .addConstraintViolation();
            }
            
            if (!malformedPlaceholders.isEmpty()) {
                context.buildConstraintViolationWithTemplate(
                    "Malformed placeholders found: " + String.join(", ", malformedPlaceholders) + 
                    ". Placeholders must be in format {PlaceholderName}")
                    .addConstraintViolation();
            }
        }
        
        return isValid;
    }
    
    private void checkForMalformedBraces(String value, Set<String> malformedPlaceholders) {
        int openBraces = 0;
        int closeBraces = 0;
        
        for (char c : value.toCharArray()) {
            if (c == '{') {
                openBraces++;
            } else if (c == '}') {
                closeBraces++;
            }
        }
        
        if (openBraces != closeBraces) {
            malformedPlaceholders.add("Unmatched braces");
        }
        
        // Check for double braces or other malformed patterns
        if (value.contains("{{") || value.contains("}}")) {
            malformedPlaceholders.add("Double braces not allowed");
        }
        
        // Check for empty placeholders
        if (value.contains("{}")) {
            malformedPlaceholders.add("Empty placeholders not allowed");
        }
    }
}
