package backend.academy.linktracker.bot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkValidatorTest {

    private LinkValidator linkValidator;

    @BeforeEach
    void setUp() {
        linkValidator = new LinkValidator();
    }

    @Test
    void isValid_shouldReturnTrueForValidGitHubUrl() {
        // Given
        String url = "https://github.com/user/repo";

        // When
        boolean result = linkValidator.isValid(url);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_shouldReturnTrueForValidStackOverflowUrl() {
        // Given
        String url = "https://stackoverflow.com/questions/12345/some-question";

        // When
        boolean result = linkValidator.isValid(url);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_shouldReturnFalseForInvalidUrl() {
        // Given
        String url = "https://example.com/page";

        // When
        boolean result = linkValidator.isValid(url);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForNullUrl() {
        // When
        boolean result = linkValidator.isValid(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForEmptyUrl() {
        // When
        boolean result = linkValidator.isValid("");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForMalformedUrl() {
        // Given
        String url = "not-a-url";

        // When
        boolean result = linkValidator.isValid(url);

        // Then
        assertThat(result).isFalse();
    }
}
