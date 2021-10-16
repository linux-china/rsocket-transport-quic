package io.rsocket.transport.quic.server;

import io.rsocket.Closeable;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * An implementation of {@link Closeable} that wraps a {@link DisposableChannel}, enabling
 * close-ability and exposing the {@link DisposableChannel}'s address.
 */
public final class CloseableChannel implements Closeable {

    /**
     * For 1.0 and 1.1 compatibility: remove when RSocket requires Reactor Netty 1.0+.
     */
    private static final Method channelAddressMethod;

    static {
        try {
            channelAddressMethod = DisposableChannel.class.getMethod("address");
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Expected address method", ex);
        }
    }

    private final DisposableChannel channel;

    /**
     * Creates a new instance
     *
     * @param channel the {@link DisposableChannel} to wrap
     * @throws NullPointerException if {@code context} is {@code null}
     */
    CloseableChannel(DisposableChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
    }

    /**
     * Return local server selector channel address.
     *
     * @return local {@link InetSocketAddress}
     * @see DisposableChannel#address()
     */
    public InetSocketAddress address() {
        try {
            return (InetSocketAddress) channel.address();
        } catch (NoSuchMethodError e) {
            try {
                return (InetSocketAddress) channelAddressMethod.invoke(this.channel);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to obtain address", ex);
            }
        }
    }

    @Override
    public void dispose() {
        channel.dispose();
    }

    @Override
    public boolean isDisposed() {
        return channel.isDisposed();
    }

    @Override
    public Mono<Void> onClose() {
        return channel.onDispose();
    }
}
