package io.rsocket.transport.quic.demo;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.incubator.quic.QuicClient;
import reactor.netty.incubator.quic.QuicConnection;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class QuicClientApp {

	public static void main(String[] args) throws Exception {
		QuicSslContext clientCtx =
				QuicSslContextBuilder.forClient()
				                     .trustManager(InsecureTrustManagerFactory.INSTANCE)
				                     .applicationProtocols("rsocket/1.0")
				                     .build();

		QuicConnection client =
				QuicClient.create()
				          .bindAddress(() -> new InetSocketAddress(0))
				          .remoteAddress(() -> new InetSocketAddress("127.0.0.1", 8080))
				          .secure(clientCtx)
				          .wiretap(false)
				          .idleTimeout(Duration.ofSeconds(5))
				          .initialSettings(spec ->
				              spec.maxData(10000000)
				                  .maxStreamDataBidirectionalLocal(1000000))
				          .connectNow();

		CountDownLatch latch = new CountDownLatch(1);
		client.createStream((in, out) -> out.sendString(Mono.just("Hello World!"))
		                                    .then(in.receive()
		                                            .asString()
		                                            .doOnNext(s -> {
		                                                System.out.println("CLIENT RECEIVED: " + s);
		                                                latch.countDown();
		                                            })
		                                            .then()))
		      .subscribe();

		latch.await();
	}
}

