package com.example.wms.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wms.admin.config.GatewayInternalTokenProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link GatewayInternalTokenProperties}'s two enforcement layers independently — mirrors
 * gateway-service's {@code GatewayInternalTokenPropertiesTest} so both ends are proven to enforce
 * identical rules (min length 32, known-weak-value rejection):
 * <ul>
 *   <li>Bean Validation constraints ({@code @NotBlank}/{@code @Size}), exercised directly against a
 *   {@link Validator} — the same mechanism Spring invokes at {@code @ConfigurationProperties} binding
 *   time, without needing a full {@code @SpringBootTest} context for this specific check.</li>
 *   <li>The known-weak-value rejection in the record's compact constructor, which always runs regardless
 *   of how the record is constructed.</li>
 * </ul>
 */
class GatewayInternalTokenPropertiesTest {

    private static final String VALID_TOKEN = "a-randomly-generated-secret-that-is-plenty-long-enough";

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsAStrongEnoughToken() {
        GatewayInternalTokenProperties properties = new GatewayInternalTokenProperties(VALID_TOKEN);
        Set<ConstraintViolation<GatewayInternalTokenProperties>> violations = validator.validate(properties);
        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsBlankToken() {
        Set<ConstraintViolation<GatewayInternalTokenProperties>> violations =
                validator.validate(new GatewayInternalTokenProperties(""));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsTokenShorterThan32Characters() {
        Set<ConstraintViolation<GatewayInternalTokenProperties>> violations =
                validator.validate(new GatewayInternalTokenProperties("too-short-for-a-shared-secret"));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsKnownInsecureDefaultValueOnConstruction() {
        assertThatThrownBy(() -> new GatewayInternalTokenProperties("local-dev-gateway-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("local-dev-gateway-token");
    }
}
