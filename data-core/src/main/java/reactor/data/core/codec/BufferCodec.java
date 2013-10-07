package reactor.data.core.codec;

import reactor.io.Buffer;

/**
 * @author Jon Brisbin
 */
public interface BufferCodec<V> extends Codec<Buffer, Buffer, V> {
}
