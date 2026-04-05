package backend.academy.linktracker.scrapper.model;

public record LinkProcessingResult(String url, boolean success, String errorMessage) {

    public static LinkProcessingResult success(String url) {
        return new LinkProcessingResult(url, true, null);
    }

    public static LinkProcessingResult failure(String url, String errorMessage) {
        return new LinkProcessingResult(url, false, errorMessage);
    }
}
