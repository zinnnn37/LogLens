package S13P31A306.loglens.domain.analysis.service.impl;

import S13P31A306.loglens.domain.analysis.dto.response.ValidationResult;
import S13P31A306.loglens.domain.analysis.service.HtmlValidationService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * HTML 검증 서비스 구현체
 */
@Slf4j
@Service
public class HtmlValidationServiceImpl implements HtmlValidationService {

    // 허용된 CDN 목록
    private static final List<String> WHITELISTED_CDNS = Arrays.asList(
            "cdn.tailwindcss.com",
            "cdn.jsdelivr.net",
            "cdnjs.cloudflare.com",
            "unpkg.com"
    );

    @Override
    public ValidationResult validate(String html) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (html == null || html.isBlank()) {
            errors.add("HTML content is empty");
            return ValidationResult.builder()
                    .isValid(false)
                    .errors(errors)
                    .warnings(warnings)
                    .build();
        }

        try {
            // 1. Jsoup으로 HTML 파싱
            Document doc = Jsoup.parse(html);

            // 2. 필수 태그 확인
            if (doc.select("html").isEmpty()) {
                errors.add("Missing <html> tag");
            }

            if (doc.select("body").isEmpty()) {
                errors.add("Missing <body> tag");
            }

            if (doc.select("head").isEmpty()) {
                warnings.add("Missing <head> tag");
            }

            // 3. 타이틀 확인
            if (doc.select("title").isEmpty()) {
                warnings.add("Missing <title> tag");
            }

            // 4. 스크립트 소스 확인
            Elements scripts = doc.select("script[src]");
            for (Element script : scripts) {
                String src = script.attr("src");
                if (!src.isBlank() && !isWhitelistedCdn(src)) {
                    warnings.add("Non-whitelisted script source: " + src);
                }
            }

            // 5. 인라인 스크립트 경고 (보안 검토 필요)
            Elements inlineScripts = doc.select("script:not([src])");
            if (!inlineScripts.isEmpty()) {
                log.debug("Found {} inline scripts (for chart rendering etc.)", inlineScripts.size());
            }

            // 6. 스타일시트 확인
            Elements stylesheets = doc.select("link[rel=stylesheet]");
            for (Element stylesheet : stylesheets) {
                String href = stylesheet.attr("href");
                if (!href.isBlank() && !isWhitelistedCdn(href)) {
                    warnings.add("Non-whitelisted stylesheet: " + href);
                }
            }

            log.info("HTML validation completed: errors={}, warnings={}", errors.size(), warnings.size());

        } catch (Exception e) {
            log.error("HTML parsing failed: {}", e.getMessage(), e);
            errors.add("HTML parsing failed: " + e.getMessage());
        }

        return ValidationResult.builder()
                .isValid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    @Override
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        try {
            // Jsoup Safelist를 사용한 sanitization
            // relaxed() 프리셋: 일반적인 텍스트 포맷팅 태그 허용
            Safelist safelist = Safelist.relaxed()
                    // 차트 렌더링을 위한 태그 추가
                    .addTags("canvas", "script", "style")
                    // script 태그 속성 허용 (CDN 로딩용)
                    .addAttributes("script", "src", "type", "charset")
                    // 스타일 태그 허용
                    .addAttributes("style", "type")
                    // 일반적인 HTML5 속성 허용
                    .addAttributes(":all", "class", "id", "style")
                    // CDN 프로토콜 허용
                    .addProtocols("script", "src", "https")
                    .addProtocols("link", "href", "https");

            String cleaned = Jsoup.clean(html, safelist);
            log.debug("HTML sanitization completed");
            return cleaned;

        } catch (Exception e) {
            log.error("HTML sanitization failed: {}", e.getMessage(), e);
            // 실패 시 원본 반환 (이미 검증 통과한 경우)
            return html;
        }
    }

    /**
     * CDN 화이트리스트 체크
     */
    private boolean isWhitelistedCdn(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        return WHITELISTED_CDNS.stream()
                .anyMatch(url::contains);
    }
}
