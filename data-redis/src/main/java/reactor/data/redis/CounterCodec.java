package reactor.data.redis;

import com.lambdaworks.redis.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * @author Jon Brisbin
 */
public class CounterCodec extends RedisCodec<String, Long> {

	private final CharsetDecoder decoder = Charset.defaultCharset().newDecoder();

	@Override
	public String decodeKey(ByteBuffer bytes) {
		try {
			return decoder.decode(bytes).toString();
		} catch(CharacterCodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Long decodeValue(ByteBuffer bytes) {
		try {
			return Long.valueOf(decoder.decode(bytes).toString());
		} catch(CharacterCodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public byte[] encodeKey(String key) {
		return key.getBytes();
	}

	@Override
	public byte[] encodeValue(Long value) {
		return String.valueOf(value).getBytes();
	}

}
