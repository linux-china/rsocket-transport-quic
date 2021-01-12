package io.rsocket.transport.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.SocketAddress;

/**
 * QUIC duplex connection
 *
 * @author linux_china
 */
public class QuicDuplexConnection implements DuplexConnection {
    @Override
    public void sendFrame(int streamId, ByteBuf frame) {

    }

    @Override
    public void sendErrorAndClose(RSocketErrorException errorException) {

    }

    @Override
    public Flux<ByteBuf> receive() {
        return null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public Mono<Void> onClose() {
        return null;
    }

    @Override
    public void dispose() {

    }
}
