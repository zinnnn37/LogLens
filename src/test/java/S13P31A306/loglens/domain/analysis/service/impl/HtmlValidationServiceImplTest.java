package S13P31A306.loglens.domain.analysis.service.impl;

import S13P31A306.loglens.domain.analysis.dto.response.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HtmlValidationService 테스트")
class HtmlValidationServiceImplTest {

    private HtmlValidationServiceImpl htmlValidationService;

    @BeforeEach
    void setup() {
        htmlValidationService = new HtmlValidationServiceImpl();
    }

    @Nested
    @DisplayName("HTML 검증 테스트")
    class ValidateTest {

        @Test
        @DisplayName("유효한 HTML이면 검증을 통과한다")
        void 유효한_HTML이면_검증을_통과한다() {
            // given
            String validHtml = """
                    <!DOCTYPE html>
                    <html lang="ko">
                    <head>
                        <meta charset="UTF-8">
                        <title>테스트 문서</title>
                    </head>
                    <body>
                        <h1>프로젝트 분석 보고서</h1>
                        <p>내용</p>
                    </body>
                    </html>
                    """;

            // when
            ValidationResult result = htmlValidationService.validate(validHtml);

            // then
            assertThat(result.getIsValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("title 태그가 없으면 경고가 발생한다")
        void title_태그가_없으면_경고가_발생한다() {
            // given
            String htmlWithoutTitle = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                    </head>
                    <body>
                        <h1>내용</h1>
                    </body>
                    </html>
                    """;

            // when
            ValidationResult result = htmlValidationService.validate(htmlWithoutTitle);

            // then
            assertThat(result.getIsValid()).isTrue();
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("Missing <title> tag"));
        }

        @Test
        @DisplayName("화이트리스트 CDN은 허용된다")
        void 화이트리스트_CDN은_허용된다() {
            // given
            String htmlWithCdn = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <script src="https://cdn.tailwindcss.com"></script>
                        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                    </head>
                    <body>
                        <h1>내용</h1>
                    </body>
                    </html>
                    """;

            // when
            ValidationResult result = htmlValidationService.validate(htmlWithCdn);

            // then
            assertThat(result.getIsValid()).isTrue();
            assertThat(result.getWarnings()).noneMatch(w -> w.contains("Non-whitelisted"));
        }

        @Test
        @DisplayName("html 태그가 없으면 검증이 실패한다")
        void html_태그가_없으면_검증이_실패한다() {
            // given
            String htmlWithoutHtmlTag = """
                    <head>
                        <title>Test</title>
                    </head>
                    <body>
                        <h1>내용</h1>
                    </body>
                    """;

            // when
            ValidationResult result = htmlValidationService.validate(htmlWithoutHtmlTag);

            // then
            assertThat(result.getIsValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("Missing <html> tag"));
        }

        @Test
        @DisplayName("body 태그가 없으면 검증이 실패한다")
        void body_태그가_없으면_검증이_실패한다() {
            // given
            String htmlWithoutBody = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Test</title>
                    </head>
                    </html>
                    """;

            // when
            ValidationResult result = htmlValidationService.validate(htmlWithoutBody);

            // then
            assertThat(result.getIsValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("Missing <body> tag"));
        }

        @Test
        @DisplayName("비화이트리스트 CDN은 경고가 발생한다")
        void 비화이트리스트_CDN은_경고가_발생한다() {
            // given
            String htmlWithUnknownCdn = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <script src="https://unknown-cdn.com/script.js"></script>
                    </head>
                    <body>
                        <h1>내용</h1>
                    </body>
                    </html>
                    """;

            // when
            ValidationResult result = htmlValidationService.validate(htmlWithUnknownCdn);

            // then
            assertThat(result.getIsValid()).isTrue();
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("Non-whitelisted script source"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("HTML이 null이거나 빈 문자열이면 검증이 실패한다")
        void HTML이_null이거나_빈_문자열이면_검증이_실패한다(String html) {
            // when
            ValidationResult result = htmlValidationService.validate(html);

            // then
            assertThat(result.getIsValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("HTML content is empty"));
        }

        @ParameterizedTest
        @MethodSource("provideValidHtmlSamples")
        @DisplayName("다양한 유효한 HTML 샘플을 검증한다")
        void 다양한_유효한_HTML_샘플을_검증한다(String html) {
            // when
            ValidationResult result = htmlValidationService.validate(html);

            // then
            assertThat(result.getIsValid()).isTrue();
        }

        private static Stream<Arguments> provideValidHtmlSamples() {
            return Stream.of(
                    Arguments.of("""
                            <!DOCTYPE html>
                            <html><head><title>Test</title></head>
                            <body><canvas id="chart"></canvas></body></html>
                            """),
                    Arguments.of("""
                            <!DOCTYPE html>
                            <html><head><style>body{margin:0}</style></head>
                            <body><div>Content</div></body></html>
                            """)
            );
        }

        @ParameterizedTest
        @MethodSource("provideInvalidHtmlSamples")
        @DisplayName("필수 태그가 없으면 검증이 실패한다")
        void 필수_태그가_없으면_검증이_실패한다(String html, String expectedError) {
            // when
            ValidationResult result = htmlValidationService.validate(html);

            // then
            assertThat(result.getIsValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains(expectedError));
        }

        private static Stream<Arguments> provideInvalidHtmlSamples() {
            return Stream.of(
                    Arguments.of("<body><h1>No HTML tag</h1></body>", "Missing <html> tag"),
                    Arguments.of("<html><head><title>No Body</title></head></html>", "Missing <body> tag")
            );
        }
    }

    @Nested
    @DisplayName("HTML Sanitization 테스트")
    class SanitizeTest {

        @Test
        @DisplayName("허용된 태그는 유지된다")
        void 허용된_태그는_유지된다() {
            // given
            String html = """
                    <html>
                    <body>
                        <h1>제목</h1>
                        <p>내용</p>
                        <canvas id="chart"></canvas>
                        <style>body{margin:0}</style>
                    </body>
                    </html>
                    """;

            // when
            String sanitized = htmlValidationService.sanitize(html);

            // then
            assertThat(sanitized).contains("<h1>");
            assertThat(sanitized).contains("<p>");
            assertThat(sanitized).contains("<canvas");
        }

        @Test
        @DisplayName("null HTML은 그대로 반환된다")
        void null_HTML은_그대로_반환된다() {
            // when
            String result = htmlValidationService.sanitize(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 HTML은 그대로 반환된다")
        void 빈_HTML은_그대로_반환된다() {
            // when
            String result = htmlValidationService.sanitize("");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("class 속성은 유지된다")
        void class_속성은_유지된다() {
            // given
            String html = "<div class='container'>Content</div>";

            // when
            String sanitized = htmlValidationService.sanitize(html);

            // then
            assertThat(sanitized).contains("class=\"container\"");
        }

        @Test
        @DisplayName("style 속성은 유지된다")
        void style_속성은_유지된다() {
            // given
            String html = "<div style='color: red;'>Content</div>";

            // when
            String sanitized = htmlValidationService.sanitize(html);

            // then
            assertThat(sanitized).contains("style=");
        }
    }
}
