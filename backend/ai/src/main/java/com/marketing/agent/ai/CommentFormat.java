package com.marketing.agent.ai;

import java.util.regex.Pattern;

public final class CommentFormat {
    private static final Pattern LINK = Pattern.compile(
            "(?i)https?://|www\\.|(?<![a-z0-9-])(?:[a-z0-9-]+\\.)+(?:[a-z]{2,63}|xn--[a-z0-9-]+)(?![a-z0-9-])");

    private CommentFormat() {}

    public static boolean isValid(String text) {
        int length = text.codePointCount(0, text.length());
        return length >= 40 && length <= 120 && !text.contains("#") && !text.contains("＃")
                && !LINK.matcher(text).find();
    }
}
