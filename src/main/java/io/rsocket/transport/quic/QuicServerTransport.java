package io.rsocket.transport.quic;

import io.rsocket.Closeable;
import io.rsocket.transport.ServerTransport;
import reactor.core.publisher.Mono;

/**
 * QUIC server transport
 *
 * @author linux_china
 */
public class QuicServerTransport implements ServerTransport<Closeable> {

    @Override
    public Mono<Closeable> start(ConnectionAcceptor acceptor) {
        return null;
    }
}
