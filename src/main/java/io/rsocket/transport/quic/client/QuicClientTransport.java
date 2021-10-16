package io.rsocket.transport.quic.client;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamType;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import reactor.core.publisher.Mono;
import reactor.netty.incubator.quic.QuicClient;
import reactor.netty.incubator.quic.QuicConnection;

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
        final QuicConnection quicConnection = client.streamObserve((conn, state) -> {
                    if (state == CONNECTED) {
                        System.out.println("connected with " + conn.getClass().getCanonicalName());
                        //conn.addHandlerLast(new RSocketLengthCodec(maxFrameLength));
                    }
                })
                .connectNow();
        final QuicDuplexConnection duplexConnection = new QuicDuplexConnection();
        quicConnection.createStream(QuicStreamType.BIDIRECTIONAL, (quicInbound, quicOutbound) -> {
            quicInbound.withConnection(connection -> {
                duplexConnection.setConnection(connection);
                duplexConnection.setInbound(quicInbound);
                duplexConnection.setOutbound(quicOutbound);
            });
            return quicOutbound;
        }).block();
        return Mono.just(duplexConnection);
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
                .idleTimeout(Duration.ofSeconds(50))
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
