package backend.academy.linktracker.scrapper.model;

import java.time.Instant;

public record LinkUpdateInfo(
        UpdateType type,
        String title,
        String author,
        Instant createdAt,
        String preview,
        String url) {

    public enum UpdateType {
        GITHUB_ISSUE,
        GITHUB_PULL_REQUEST,
        STACKOVERFLOW_ANSWER,
        STACKOVERFLOW_COMMENT,
        GENERIC_UPDATE
    }

    public String formatMessage() {
        return switch (type) {
            case GITHUB_ISSUE -> formatGitHubIssue();
            case GITHUB_PULL_REQUEST -> formatGitHubPullRequest();
            case STACKOVERFLOW_ANSWER -> formatStackOverflowAnswer();
            case STACKOVERFLOW_COMMENT -> formatStackOverflowComment();
            case GENERIC_UPDATE -> "Link updated: " + url;
        };
    }

    private String formatGitHubIssue() {
        return String.format(
                "New GitHub Issue\n\n" +
                        "Title: %s\n" +
                        "Author: %s\n" +
                        "Created: %s\n" +
                        "Preview: %s\n\n" +
                        "Link: %s",
                title, author, createdAt, preview, url);
    }

    private String formatGitHubPullRequest() {
        return String.format(
                "New GitHub Pull Request\n\n" +
                        "Title: %s\n" +
                        "Author: %s\n" +
                        "Created: %s\n" +
                        "Preview: %s\n\n" +
                        "Link: %s",
                title, author, createdAt, preview, url);
    }

    private String formatStackOverflowAnswer() {
        return String.format(
                "New StackOverflow Answer\n\n" +
                        "Question: %s\n" +
                        "Author: %s\n" +
                        "Created: %s\n" +
                        "Preview: %s\n\n" +
                        "Link: %s",
                title, author, createdAt, preview, url);
    }

    private String formatStackOverflowComment() {
        return String.format(
                "New StackOverflow Comment\n\n" +
                        "Question: %s\n" +
                        "Author: %s\n" +
                        "Created: %s\n" +
                        "Preview: %s\n\n" +
                        "Link: %s",
                title, author, createdAt, preview, url);
    }
}
