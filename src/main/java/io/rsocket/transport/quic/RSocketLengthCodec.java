package io.rsocket.transport.quic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_SIZE;

public class RSocketLengthCodec extends LengthFieldBasedFrameDecoder {
    /**
     * Creates a new instance of the decoder, specifying the RSocket frame length header size.
     */
    public RSocketLengthCodec() {
        this(FRAME_LENGTH_MASK);
    }

    /**
     * Creates a new instance of the decoder, specifying the RSocket frame length header size.
     *
     * @param maxFrameLength maximum allowed frame length for incoming rsocket frames
     */
    public RSocketLengthCodec(int maxFrameLength) {
        super(maxFrameLength, 0, FRAME_LENGTH_SIZE, 0, 0);
    }

    /**
     * Simplified non-netty focused decode usage.
     *
     * @param in the input buffer to read data from.
     * @return decoded buffer or null is none available.
     * @throws Exception if any error happens.
     * @see #decode(ChannelHandlerContext, ByteBuf)
     */
    public Object decode(ByteBuf in) throws Exception {
        return decode(null, in);
    }
}
