package backend.academy.linktracker.bot.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class UpdateServiceGrpc {

  private UpdateServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "linktracker.UpdateService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<backend.academy.linktracker.bot.grpc.LinkUpdateRequest,
      backend.academy.linktracker.bot.grpc.LinkUpdateResponse> getSendUpdateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendUpdate",
      requestType = backend.academy.linktracker.bot.grpc.LinkUpdateRequest.class,
      responseType = backend.academy.linktracker.bot.grpc.LinkUpdateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<backend.academy.linktracker.bot.grpc.LinkUpdateRequest,
      backend.academy.linktracker.bot.grpc.LinkUpdateResponse> getSendUpdateMethod() {
    io.grpc.MethodDescriptor<backend.academy.linktracker.bot.grpc.LinkUpdateRequest, backend.academy.linktracker.bot.grpc.LinkUpdateResponse> getSendUpdateMethod;
    if ((getSendUpdateMethod = UpdateServiceGrpc.getSendUpdateMethod) == null) {
      synchronized (UpdateServiceGrpc.class) {
        if ((getSendUpdateMethod = UpdateServiceGrpc.getSendUpdateMethod) == null) {
          UpdateServiceGrpc.getSendUpdateMethod = getSendUpdateMethod =
              io.grpc.MethodDescriptor.<backend.academy.linktracker.bot.grpc.LinkUpdateRequest, backend.academy.linktracker.bot.grpc.LinkUpdateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendUpdate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  backend.academy.linktracker.bot.grpc.LinkUpdateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  backend.academy.linktracker.bot.grpc.LinkUpdateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UpdateServiceMethodDescriptorSupplier("SendUpdate"))
              .build();
        }
      }
    }
    return getSendUpdateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UpdateServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UpdateServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UpdateServiceStub>() {
        @java.lang.Override
        public UpdateServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UpdateServiceStub(channel, callOptions);
        }
      };
    return UpdateServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static UpdateServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UpdateServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UpdateServiceBlockingV2Stub>() {
        @java.lang.Override
        public UpdateServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UpdateServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return UpdateServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UpdateServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UpdateServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UpdateServiceBlockingStub>() {
        @java.lang.Override
        public UpdateServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UpdateServiceBlockingStub(channel, callOptions);
        }
      };
    return UpdateServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static UpdateServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UpdateServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UpdateServiceFutureStub>() {
        @java.lang.Override
        public UpdateServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UpdateServiceFutureStub(channel, callOptions);
        }
      };
    return UpdateServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void sendUpdate(backend.academy.linktracker.bot.grpc.LinkUpdateRequest request,
        io.grpc.stub.StreamObserver<backend.academy.linktracker.bot.grpc.LinkUpdateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendUpdateMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service UpdateService.
   */
  public static abstract class UpdateServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return UpdateServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service UpdateService.
   */
  public static final class UpdateServiceStub
      extends io.grpc.stub.AbstractAsyncStub<UpdateServiceStub> {
    private UpdateServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UpdateServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UpdateServiceStub(channel, callOptions);
    }

    /**
     */
    public void sendUpdate(backend.academy.linktracker.bot.grpc.LinkUpdateRequest request,
        io.grpc.stub.StreamObserver<backend.academy.linktracker.bot.grpc.LinkUpdateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendUpdateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service UpdateService.
   */
  public static final class UpdateServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<UpdateServiceBlockingV2Stub> {
    private UpdateServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UpdateServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UpdateServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public backend.academy.linktracker.bot.grpc.LinkUpdateResponse sendUpdate(backend.academy.linktracker.bot.grpc.LinkUpdateRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSendUpdateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service UpdateService.
   */
  public static final class UpdateServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<UpdateServiceBlockingStub> {
    private UpdateServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UpdateServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UpdateServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public backend.academy.linktracker.bot.grpc.LinkUpdateResponse sendUpdate(backend.academy.linktracker.bot.grpc.LinkUpdateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendUpdateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service UpdateService.
   */
  public static final class UpdateServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<UpdateServiceFutureStub> {
    private UpdateServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UpdateServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UpdateServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<backend.academy.linktracker.bot.grpc.LinkUpdateResponse> sendUpdate(
        backend.academy.linktracker.bot.grpc.LinkUpdateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendUpdateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_UPDATE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_UPDATE:
          serviceImpl.sendUpdate((backend.academy.linktracker.bot.grpc.LinkUpdateRequest) request,
              (io.grpc.stub.StreamObserver<backend.academy.linktracker.bot.grpc.LinkUpdateResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSendUpdateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              backend.academy.linktracker.bot.grpc.LinkUpdateRequest,
              backend.academy.linktracker.bot.grpc.LinkUpdateResponse>(
                service, METHODID_SEND_UPDATE)))
        .build();
  }

  private static abstract class UpdateServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    UpdateServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return backend.academy.linktracker.bot.grpc.UpdateServiceOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("UpdateService");
    }
  }

  private static final class UpdateServiceFileDescriptorSupplier
      extends UpdateServiceBaseDescriptorSupplier {
    UpdateServiceFileDescriptorSupplier() {}
  }

  private static final class UpdateServiceMethodDescriptorSupplier
      extends UpdateServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    UpdateServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (UpdateServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new UpdateServiceFileDescriptorSupplier())
              .addMethod(getSendUpdateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
