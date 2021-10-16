package io.rsocket.transport.quic;


import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.quic.client.QuicClientTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Hooks;

import java.util.Objects;

public class RSocketQuicClient {
    public static void main(String[] args) throws Exception {
        Hooks.onErrorDropped(Throwable::printStackTrace);
        RSocket clientRSocket = RSocketConnector.create()
                .connect(QuicClientTransport.create("127.0.0.1", 7878))
                //.connect(TcpClientTransport.create("127.0.0.1", 7878))
                .block();
        Objects.requireNonNull(clientRSocket, "client must not be null");
        clientRSocket.requestResponse(DefaultPayload.create("data", "metadata")).subscribe(payload -> {
            System.out.println("received: " + payload.getDataUtf8());
        });

        Thread.sleep(5000);
    }
}
