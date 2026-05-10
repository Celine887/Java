package backend.academy.linktracker.bot.grpc;

import backend.academy.linktracker.bot.service.UpdateNotificationService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UpdateServiceImpl extends UpdateServiceGrpc.UpdateServiceImplBase {

    private final UpdateNotificationService updateNotificationService;

    @Override
    public void sendUpdate(LinkUpdateRequest request, StreamObserver<LinkUpdateResponse> responseObserver) {
        log.info("Received gRPC update: url={}, chatIds={}", request.getUrl(), request.getTgChatIdsList());

        try {
            backend.academy.linktracker.bot.dto.LinkUpdateRequest dtoRequest =
                    new backend.academy.linktracker.bot.dto.LinkUpdateRequest(
                            request.getId(), request.getUrl(), request.getDescription(), request.getTgChatIdsList());

            updateNotificationService.sendNotifications(dtoRequest);

            LinkUpdateResponse response = LinkUpdateResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Update processed successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully processed gRPC update for url={}", request.getUrl());
        } catch (Exception e) {
            log.error("Error processing gRPC update", e);

            LinkUpdateResponse response = LinkUpdateResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
