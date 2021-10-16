package io.rsocket.transport.quic.server;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.rsocket.Closeable;
import io.rsocket.transport.ServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.incubator.quic.QuicServer;

import java.time.Duration;
import java.util.Objects;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

/**
 * QUIC server transport
 *
 * @author linux_china
 */
public class QuicServerTransport implements ServerTransport<Closeable> {
    private static Logger log = LoggerFactory.getLogger(QuicServerTransport.class);
    private final int port;
    private final int maxFrameLength = FRAME_LENGTH_MASK;

    public static QuicServerTransport create(int port) {
        return new QuicServerTransport(port);
    }

    public QuicServerTransport(int port) {
        this.port = port;
    }

    @Override
    public Mono<Closeable> start(ConnectionAcceptor acceptor) {
        Objects.requireNonNull(acceptor, "acceptor must not be null");
        final QuicServer quicServer = createServer();
        return quicServer
                .handleStream((inbound, outbound) -> {
                    acceptor.apply(new QuicServerDuplexConnection(inbound, outbound))
                            .subscribe();
                    log.info("acceptor initialized");
                    return outbound;
                })
                .bind()
                .map(CloseableChannel::new);
    }

    private QuicSslContext quicSslContext() {
        try {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            return QuicSslContextBuilder.forServer(ssc.privateKey(), null, ssc.certificate())
                    .applicationProtocols("rsocket/1.0")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private QuicServer createServer() {
        return QuicServer.create()
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                .host("0.0.0.0")
                .port(port)
                .wiretap(false)
                .secure(quicSslContext())
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
