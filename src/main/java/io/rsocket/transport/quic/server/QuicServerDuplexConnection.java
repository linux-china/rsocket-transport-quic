package io.rsocket.transport.quic.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketErrorException;
import io.rsocket.frame.ErrorFrameCodec;
import io.rsocket.frame.FrameLengthCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.incubator.quic.QuicInbound;
import reactor.netty.incubator.quic.QuicOutbound;

import java.net.SocketAddress;

/**
 * QUIC duplex connection
 *
 * @author linux_china
 */
public class QuicServerDuplexConnection implements DuplexConnection {
    private static Logger log = LoggerFactory.getLogger(QuicServerDuplexConnection.class);
    protected Sinks.Empty<Void> onClose = Sinks.empty();
    //    private final Connection connection;
    private QuicInbound inbound;
    private QuicOutbound outbound;

    /**
     * Creates a new instance
     */
    public QuicServerDuplexConnection(QuicInbound inbound, QuicOutbound outbound) {
        log.info("server connection created: " + inbound.getClass().getCanonicalName());
        this.inbound = inbound;
        this.outbound = outbound;
    }

    @Override
    public ByteBufAllocator alloc() {
        return outbound.alloc();
    }

    @Override
    public SocketAddress remoteAddress() {
        //return connection.channel().remoteAddress();
        return null;
    }


    @Override
    public void sendErrorAndClose(RSocketErrorException e) {
        final ByteBuf errorFrame = ErrorFrameCodec.encode(alloc(), 0, e);
        outbound
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
        log.info("Begin to receive buffers from client!");
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
        //connection.dispose();
        onClose.tryEmitEmpty();
    }
}
