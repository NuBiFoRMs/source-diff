package com.nubiform.sourcediff.validator;

import com.nubiform.sourcediff.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Slf4j
@RequiredArgsConstructor
@Component
public class RepositoryValidator implements Validator {

    private final AppProperties appProperties;

    @Override
    public boolean supports(Class<?> clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        String repository = (String) target;
        log.debug("validate: {}", repository);

        if (appProperties.getRepositories()
                .stream()
                .map(AppProperties.RepositoryProperties::getName)
                .noneMatch(name -> name.equals(repository))) {
            errors.reject("repository");
        }
    }
}
