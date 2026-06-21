package com.knowvault.ai;

public interface AIProvider {
    AIResponse summarize(String content, SummaryLength length);
}
