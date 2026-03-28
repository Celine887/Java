package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.bot.grpc.LinkUpdateRequest;
import backend.academy.linktracker.bot.grpc.LinkUpdateResponse;
import backend.academy.linktracker.bot.grpc.UpdateServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("grpc")
public class GrpcUpdateClient implements UpdateClient {
    private static final Logger log = LoggerFactory.getLogger(GrpcUpdateClient.class);

    @Value("${app.bot.grpc.host:localhost}")
    private String botHost;

    @Value("${app.bot.grpc.port:9090}")
    private int botPort;

    private ManagedChannel channel;
    private UpdateServiceGrpc.UpdateServiceBlockingStub stub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress(botHost, botPort)
                .usePlaintext()
                .build();

        stub = UpdateServiceGrpc.newBlockingStub(channel);
        log.info("gRPC client initialized: {}:{}", botHost, botPort);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            log.info("gRPC channel shut down");
        }
    }

    @Override
    public void sendUpdate(backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest request) {
        try {
            LinkUpdateRequest grpcRequest = LinkUpdateRequest.newBuilder()
                    .setId(request.id())
                    .setUrl(request.url())
                    .setDescription(request.description())
                    .addAllTgChatIds(request.tgChatIds())
                    .build();

            LinkUpdateResponse response = stub.sendUpdate(grpcRequest);

            if (response.getSuccess()) {
                log.info("Successfully sent update via gRPC: url={}", request.url());
            } else {
                log.warn("gRPC update failed: url={}, message={}", request.url(), response.getMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: url={}, status={}", request.url(), e.getStatus(), e);
        } catch (Exception e) {
            log.error("Error sending update via gRPC: url={}", request.url(), e);
        }
    }
}
