package io.rsocket.transport.quic.client;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.quic.QuicDuplexConnection;
import io.rsocket.transport.quic.RSocketLengthCodec;
import reactor.core.publisher.Mono;
import reactor.netty.incubator.quic.QuicClient;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static reactor.netty.ConnectionObserver.State.CONNECTED;

/**
 * QUIC client transport
 *
 * @author linux_china
 */
public class QuicClientTransport implements ClientTransport {
    private final QuicClient client;
    private final int maxFrameLength = FRAME_LENGTH_MASK;

    private QuicClientTransport(QuicClient client) {
        this.client = client;
    }

    public static QuicClientTransport create(String bindAddress, int port) {
        Objects.requireNonNull(bindAddress, "bindAddress must not be null");
        QuicClient quicClient = createClient(() -> new InetSocketAddress(bindAddress, port));
        return create(quicClient);
    }

    public static QuicClientTransport create(QuicClient quicClient) {
        return new QuicClientTransport(quicClient);
    }


    @Override
    public Mono<DuplexConnection> connect() {
        return client.streamObserve((conn, state) -> {
                    System.out.println("stream observer");
                    if (state == CONNECTED) {
                        conn.addHandlerLast(new RSocketLengthCodec(maxFrameLength));
                    }
                })
                .connect()
                .map(QuicDuplexConnection::new);
    }

    static QuicClient createClient(Supplier<SocketAddress> remoteAddress) {
        QuicSslContext clientCtx = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols("rsocket/1.0")
                .build();
        return QuicClient.create()
                .remoteAddress(remoteAddress)
                .bindAddress(() -> new InetSocketAddress(0))
                .wiretap(false)
                .secure(clientCtx)
                .idleTimeout(Duration.ofSeconds(5))
                .initialSettings(spec -> {
                    spec.maxData(10000000)
                            .maxStreamDataBidirectionalLocal(1000000)
                            .maxStreamDataBidirectionalRemote(1000000)
                            .maxStreamDataUnidirectional(1000000)
                            .maxStreamsBidirectional(100)
                            .maxStreamsUnidirectional(100);
                });
    }
}
