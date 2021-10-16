package io.rsocket.transport.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketErrorException;
import io.rsocket.frame.ErrorFrameCodec;
import io.rsocket.frame.FrameLengthCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;

import java.net.SocketAddress;

/**
 * QUIC duplex connection
 *
 * @author linux_china
 */
public class QuicDuplexConnection implements DuplexConnection {
    protected Sinks.Empty<Void> onClose = Sinks.empty();
    private final Connection connection;

    /**
     * Creates a new instance
     */
    public QuicDuplexConnection(Connection connection) {
        super();
        this.connection = connection;
        this.connection
                .channel()
                .closeFuture()
                .addListener(
                        future -> {
                            if (!isDisposed()) dispose();
                        });
    }

    @Override
    public ByteBufAllocator alloc() {
        return connection.channel().alloc();
    }

    @Override
    public SocketAddress remoteAddress() {
        return connection.channel().remoteAddress();
    }


    @Override
    public void sendErrorAndClose(RSocketErrorException e) {
        final ByteBuf errorFrame = ErrorFrameCodec.encode(alloc(), 0, e);
        connection.outbound()
                .sendObject(FrameLengthCodec.encode(alloc(), errorFrame.readableBytes(), errorFrame))
                .then()
                .subscribe(
                        null,
                        t -> onClose.tryEmitError(t),
                        () -> {
                            final Throwable cause = e.getCause();
                            if (cause == null) {
                                onClose.tryEmitEmpty();
                            } else {
                                onClose.tryEmitError(cause);
                            }
                        });
    }

    @Override
    public Flux<ByteBuf> receive() {
        //todo ReactorNetty.unavailableInbound(this);
        return connection.inbound().receive().map(FrameLengthCodec::frame);
    }

    @Override
    public void sendFrame(int streamId, ByteBuf frame) {
        connection.outbound().send(Mono.just(FrameLengthCodec.encode(alloc(), frame.readableBytes(), frame)));
    }

    @Override
    public Mono<Void> onClose() {
        return onClose.asMono();
    }

    @Override
    public void dispose() {
        connection.dispose();
        onClose.tryEmitEmpty();
    }
}
