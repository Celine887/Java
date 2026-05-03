package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StackOverflowLinkChecker implements LinkChecker {

    private static final Pattern STACKOVERFLOW_PATTERN =
            Pattern.compile("https://stackoverflow\\.com/questions/(\\d+)/[\\w-]+/?");

    private final StackOverflowClient stackOverflowClient;

    @Override
    public boolean supports(String url) {
        return STACKOVERFLOW_PATTERN.matcher(url).matches();
    }

    @Override
    public Optional<Instant> getLastUpdate(String url) {
        Matcher matcher = STACKOVERFLOW_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        Long questionId = Long.parseLong(matcher.group(1));

        return stackOverflowClient.fetchQuestion(questionId).map(question -> question.getLastActivityInstant());
    }

    @Override
    public List<LinkUpdateInfo> checkForUpdates(String url, Instant lastChecked) {
        Matcher matcher = STACKOVERFLOW_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return List.of();
        }

        Long questionId = Long.parseLong(matcher.group(1));
        List<LinkUpdateInfo> updates = new ArrayList<>();

        String questionTitle = stackOverflowClient
                .fetchQuestion(questionId)
                .map(q -> q.title())
                .orElse("Question #" + questionId);

        List<StackOverflowAnswerResponse.AnswerItem> answers =
                stackOverflowClient.fetchAnswers(questionId, lastChecked);
        for (StackOverflowAnswerResponse.AnswerItem answer : answers) {
            String answerUrl = String.format("https://stackoverflow.com/a/%d", answer.answerId());
            updates.add(new LinkUpdateInfo(
                    LinkUpdateInfo.UpdateType.STACKOVERFLOW_ANSWER,
                    questionTitle,
                    answer.owner().displayName(),
                    answer.getCreationInstant(),
                    answer.getPreview(),
                    answerUrl));
        }

        List<StackOverflowCommentResponse.CommentItem> comments =
                stackOverflowClient.fetchComments(questionId, lastChecked);
        for (StackOverflowCommentResponse.CommentItem comment : comments) {
            String commentUrl = String.format(
                    "https://stackoverflow.com/questions/%d#comment%d_%d",
                    questionId, comment.commentId(), comment.postId());
            updates.add(new LinkUpdateInfo(
                    LinkUpdateInfo.UpdateType.STACKOVERFLOW_COMMENT,
                    questionTitle,
                    comment.owner().displayName(),
                    comment.getCreationInstant(),
                    comment.getPreview(),
                    commentUrl));
        }

        log.debug("Found {} updates for StackOverflow question: {}", updates.size(), questionId);
        return updates;
    }
}
