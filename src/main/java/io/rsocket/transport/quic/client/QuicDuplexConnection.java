package io.rsocket.transport.quic.client;

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
import reactor.netty.NettyOutbound;

import java.net.SocketAddress;

/**
 * QUIC duplex connection
 *
 * @author linux_china
 */
public class QuicDuplexConnection implements DuplexConnection {
    protected Sinks.Empty<Void> onClose = Sinks.empty();
    private  Connection connection;
    private NettyInbound inbound;
    private NettyOutbound outbound;

    public QuicDuplexConnection() {
    }

    /**
     * Creates a new instance
     */
    public QuicDuplexConnection(Connection connection, NettyInbound inbound, NettyOutbound outbound) {
        System.out.println("client connection created and base on : " + connection.getClass().getCanonicalName());
        this.connection = connection;
        this.inbound = inbound;
        this.outbound = outbound;
        this.connection
                .channel()
                .closeFuture()
                .addListener(
                        future -> {
                            if (!isDisposed()) dispose();
                        });
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setInbound(NettyInbound inbound) {
        this.inbound = inbound;
    }

    public void setOutbound(NettyOutbound outbound) {
        this.outbound = outbound;
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
        System.out.println("Begin to receive buffers from server!");
        return inbound.receive().map(FrameLengthCodec::frame);
    }

    @Override
    public void sendFrame(int streamId, ByteBuf frame) {
        System.out.println("Begin to send buffers to server!");
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
