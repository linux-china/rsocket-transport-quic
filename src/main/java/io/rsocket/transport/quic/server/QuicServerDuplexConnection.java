package io.rsocket.transport.quic.server;

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
import reactor.netty.incubator.quic.QuicInbound;
import reactor.netty.incubator.quic.QuicOutbound;

import java.net.SocketAddress;

/**
 * QUIC duplex connection
 *
 * @author linux_china
 */
public class QuicServerDuplexConnection implements DuplexConnection {
    protected Sinks.Empty<Void> onClose = Sinks.empty();
    private final Connection connection;
    private QuicInbound inbound;
    private QuicOutbound outbound;

    /**
     * Creates a new instance
     */
    public QuicServerDuplexConnection(Connection connection) {
        this.connection = connection;
        this.inbound = (QuicInbound) connection.inbound();
        this.outbound = (QuicOutbound) connection.outbound();
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
        System.out.println("Begin to receive buffers from client!");
        return inbound.receive().map(FrameLengthCodec::frame);
    }

    @Override
    public void sendFrame(int streamId, ByteBuf frame) {
        outbound.send(Mono.just(FrameLengthCodec.encode(alloc(), frame.readableBytes(), frame)));
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
