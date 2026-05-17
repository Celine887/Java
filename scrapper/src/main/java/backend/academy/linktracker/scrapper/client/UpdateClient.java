package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;

public interface UpdateClient {
    void sendUpdate(LinkUpdateRequest request);
}
