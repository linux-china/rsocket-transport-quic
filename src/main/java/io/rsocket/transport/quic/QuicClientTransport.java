package io.rsocket.transport.quic;

import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import reactor.core.publisher.Mono;

/**
 * QUIC client transport
 *
 * @author linux_china
 */
public class QuicClientTransport implements ClientTransport {
    @Override
    public Mono<DuplexConnection> connect() {
        return null;
    }
}
