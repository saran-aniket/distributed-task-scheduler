package com.personal.distributedtaskscheduler.controller;

import com.personal.distributedtaskscheduler.dto.CreateJobRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JobControllerValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void postCreateJob_withValidCronExpression_passesValidation() {
        assertValidCron("0 0 * * *");
    }

    @Test
    void postCreateJob_withSixFieldCronExpression_failsValidation() {
        assertInvalidCron("0 0 0 * * *");
    }

    @Test
    void postCreateJob_withTooManyFields_failsValidation() {
        assertInvalidCron("0 0 * * * *");
    }

    @Test
    void postCreateJob_withTooFewFields_failsValidation() {
        assertInvalidCron("0 0 * *");
    }

    @Test
    void postCreateJob_withIllegalCharacters_failsValidation() {
        assertInvalidCron("0 0 @ * *");
    }

    @Test
    void postCreateJob_withQuestionMark_failsValidation() {
        assertInvalidCron("0 0 ? * *");
    }

    @Test
    void postCreateJob_withMalformedRange_failsValidation() {
        assertInvalidCron("0 0 *- * *");
    }

    @Test
    void postCreateJob_withMalformedStep_failsValidation() {
        assertInvalidCron("0 0 0/ * *");
    }

    @Test
    void postCreateJob_withNullCronExpression_passesValidation() {
        CreateJobRequestDTO request = baseRequest();
        request.setCronExpression(null);

        Set<ConstraintViolation<CreateJobRequestDTO>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void postCreateJob_withEveryVariationOfValidCron_allPass() {
        String[] validCrons = {
                "0 0 * * *",
                "*/15 * * * *",
                "0 9 1 * *",
                "30 2 * * 1-5",
                "0 0,12 * * *",
                "0 9-17 * * 1-5",
                "0 22 * * 5",
                "59 23 31 12 6",
        };

        for (String cron : validCrons) {
            assertValidCron(cron);
        }
    }

    private void assertValidCron(String cron) {
        CreateJobRequestDTO request = baseRequest();
        request.setCronExpression(cron);

        Set<ConstraintViolation<CreateJobRequestDTO>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    private void assertInvalidCron(String cron) {
        CreateJobRequestDTO request = baseRequest();
        request.setCronExpression(cron);

        Set<ConstraintViolation<CreateJobRequestDTO>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("cronExpression"));
    }

    private CreateJobRequestDTO baseRequest() {
        CreateJobRequestDTO request = new CreateJobRequestDTO();
        request.setName("job");
        request.setJobType("HTTP_CALLBACK");
        request.setPayLoad("{}");
        return request;
    }
}
