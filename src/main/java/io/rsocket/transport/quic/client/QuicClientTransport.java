package io.rsocket.transport.quic.client;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.quic.RSocketLengthCodec;
import reactor.core.publisher.Mono;
import reactor.netty.incubator.quic.QuicClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

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
        QuicClient quicClient = QuicClient.create().remoteAddress(() -> new InetSocketAddress(bindAddress, port));
        return create(quicClient);
    }

    public static QuicClientTransport create(QuicClient quicClient) {
        return new QuicClientTransport(quicClient);
    }


    @Override
    public Mono<DuplexConnection> connect() {
        QuicSslContext clientCtx = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols("rsocket/1.0")
                .build();
        return client
                .bindAddress(() -> new InetSocketAddress(0))
                .secure(clientCtx)
                .wiretap(false)
                .idleTimeout(Duration.ofSeconds(5))
                .initialSettings(spec -> spec.maxData(10000000).maxStreamDataBidirectionalLocal(1000000))
                .doOnConnected(c -> c.addHandlerLast(new RSocketLengthCodec(maxFrameLength)))
                .connect()
                .map(QuicDuplexClientConnection::new);
    }
}
