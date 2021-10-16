package io.rsocket.transport.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.RSocketErrorException;
import io.rsocket.frame.ErrorFrameCodec;
import io.rsocket.frame.FrameLengthCodec;
import io.rsocket.internal.BaseDuplexConnection;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;

import java.net.SocketAddress;
import java.util.Objects;

/**
 * QUIC duplex connection
 *
 * @author linux_china
 */
public class QuicDuplexConnection extends BaseDuplexConnection {
    private final Connection connection;

    /**
     * Creates a new instance
     *
     * @param connection the {@link Connection} for managing the server
     */
    public QuicDuplexConnection(Connection connection) {
        super();
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        connection
                .channel()
                .closeFuture()
                .addListener(
                        future -> {
                            if (!isDisposed()) dispose();
                        });

        connection.outbound().send(sender).then().subscribe();
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
    protected void doOnClose() {
        sender.dispose();
        connection.dispose();
    }

    @Override
    public void sendErrorAndClose(RSocketErrorException e) {
        final ByteBuf errorFrame = ErrorFrameCodec.encode(alloc(), 0, e);
        connection
                .outbound()
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
        return connection.inbound().receive().map(FrameLengthCodec::frame);
    }

    @Override
    public void sendFrame(int streamId, ByteBuf frame) {
        super.sendFrame(streamId, FrameLengthCodec.encode(alloc(), frame.readableBytes(), frame));
    }
}
