package com.easybulk.service;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
public class ContextAnalyzer {

    public ContextInfo analyze(String partialText) {
        if (partialText == null || partialText.isBlank()) {
            return new ContextInfo("next_word", "", List.of());
        }

        boolean endsWithSpace = partialText.endsWith(" ");
        String[] tokens = partialText.trim().split("\\s+");
        List<String> tokenList = Arrays.asList(tokens);

        if (endsWithSpace) {
            return new ContextInfo("next_word", "", tokenList);
        } else {
            String currentWord = tokens[tokens.length - 1];
            List<String> context = tokenList.subList(0, tokenList.size() - 1);
            return new ContextInfo("autocomplete", currentWord, context);
        }
    }

    public record ContextInfo(
            String mode,
            String currentWord,
            List<String> context
    ) {
        public String getLastWord() {
            return context.isEmpty() ? "" : context.get(context.size() - 1);
        }
        public boolean hasContext() {
            return context.size() >= 2;
        }
        public String getSecondToLastWord() {
            return context.size() >= 2 ? context.get(context.size() - 2) : "";
        }
    }
}