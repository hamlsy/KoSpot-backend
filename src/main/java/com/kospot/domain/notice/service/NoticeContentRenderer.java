package com.kospot.domain.notice.service;

import org.springframework.stereotype.Component;

@Component
public class NoticeContentRenderer {

    private final com.vladsch.flexmark.parser.Parser parser =
            com.vladsch.flexmark.parser.Parser.builder().build();

    private final com.vladsch.flexmark.html.HtmlRenderer renderer =
            com.vladsch.flexmark.html.HtmlRenderer.builder().build();

    // 최소 정책 + img 허용(https만) 예시
    private final org.owasp.html.PolicyFactory policy =
            org.owasp.html.Sanitizers.FORMATTING
                    .and(org.owasp.html.Sanitizers.BLOCKS)
                    .and(org.owasp.html.Sanitizers.LINKS)
                    .and(new org.owasp.html.HtmlPolicyBuilder()
                            .allowElements("img")
                            .allowAttributes("src").onElements("img")
                            .allowAttributes("alt").onElements("img")
                            .allowUrlProtocols("https")
                            .toFactory());

    public String renderToSafeHtml(String markdown) {
        String md = (markdown == null) ? "" : markdown;
        var document = parser.parse(md);
        String html = renderer.render(document);
        return policy.sanitize(html);
    }
}
