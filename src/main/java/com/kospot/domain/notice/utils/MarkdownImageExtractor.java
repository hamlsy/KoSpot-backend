package com.kospot.domain.notice.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownImageExtractor {
    // ![alt](url "title") 에서 url만 뽑음
    private static final Pattern MD_IMG = Pattern.compile("!\\[[^\\]]*\\]\\((\\S+)(?:\\s+\"[^\"]*\")?\\)");

    private MarkdownImageExtractor() {}

    public static Set<String> extractUrls(String markdown) {
        if (markdown == null || markdown.isBlank()) return Set.of();
        Matcher m = MD_IMG.matcher(markdown);
        Set<String> urls = new HashSet<>();
        while (m.find()) {
            String url = m.group(1);
            // 괄호 끝에 ')'가 붙는 케이스가 있으면 여기서 trim 보정
            urls.add(url.trim());
        }
        return urls;
    }
}
