package reactor.data.redis.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdaworks.redis.codec.RedisCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * @author Jon Brisbin
 */
public class ObjectMapperCodec<V> extends RedisCodec<String, V> {

	private static final Logger LOG = LoggerFactory.getLogger(ObjectMapperCodec.class);

	private final ObjectMapper   mapper  = new ObjectMapper();
	private final CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
	private final Class<V> type;

	public ObjectMapperCodec(Class<V> type) {
		this.type = type;
	}

	@Override
	public String decodeKey(ByteBuffer bytes) {
		try {
			return decoder.decode(bytes).toString();
		} catch(CharacterCodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public V decodeValue(ByteBuffer bytes) {
		try {
			String s = decoder.decode(bytes).toString();
			return mapper.readValue(s, type);
		} catch(IOException e) {
			if(LOG.isErrorEnabled()) {
				LOG.error(e.getMessage(), e);
			}
			throw new IllegalStateException(e);
		}
	}

	@Override
	public byte[] encodeKey(String key) {
		return key.getBytes();
	}

	@Override
	public byte[] encodeValue(V value) {
		try {
			return mapper.writeValueAsBytes(value);
		} catch(JsonProcessingException e) {
			if(LOG.isErrorEnabled()) {
				LOG.error(e.getMessage(), e);
			}
			throw new IllegalStateException(e);

		}
	}

}
