package reactor.data.core.codec.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.data.core.codec.BufferCodec;
import reactor.data.core.codec.ByteArrayCodec;
import reactor.data.core.codec.StringCodec;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.io.Buffer;

import java.io.IOException;

/**
 * @author Jon Brisbin
 */
public abstract class JsonCodecs {

	public static <T> ByteArrayCodec<T> byteArrayCodec(final ObjectMapper mapper, final Class<T> type) {
		return new ByteArrayCodec<T>() {
			@Override
			public Consumer<byte[]> decoder(final Consumer<T> next) {
				return new Consumer<byte[]>() {
					@Override
					public void accept(byte[] bytes) {
						try {
							next.accept(mapper.readValue(bytes, type));
						} catch(IOException e) {
							throw new IllegalArgumentException(e);
						}
					}
				};
			}

			@Override
			public Function<T, byte[]> encoder() {
				return new Function<T, byte[]>() {
					@Override
					public byte[] apply(T t) {
						try {
							return mapper.writeValueAsBytes(t);
						} catch(JsonProcessingException e) {
							throw new IllegalArgumentException(e);
						}
					}
				};
			}
		};
	}

	public static <T> StringCodec<T> stringCodec(final ObjectMapper mapper, final Class<T> type) {
		return new StringCodec<T>() {
			@Override
			public Consumer<String> decoder(final Consumer<T> next) {
				return new Consumer<String>() {
					@Override
					public void accept(String s) {
						try {
							next.accept(mapper.readValue(s, type));
						} catch(IOException e) {
							throw new IllegalArgumentException(e);
						}
					}
				};
			}

			@Override
			public Function<T, String> encoder() {
				return new Function<T, String>() {
					@Override
					public String apply(T t) {
						try {
							return mapper.writeValueAsString(t);
						} catch(JsonProcessingException e) {
							throw new IllegalArgumentException(e);
						}
					}
				};
			}
		};
	}

	public static <T> BufferCodec<T> bufferCodec(final ObjectMapper mapper, final Class<T> type) {
		return new BufferCodec<T>() {
			@Override
			public Consumer<Buffer> decoder(final Consumer<T> next) {
				return new Consumer<Buffer>() {
					@Override
					public void accept(Buffer bytes) {
						try {
							next.accept(mapper.readValue(bytes.asBytes(), type));
						} catch(IOException e) {
							throw new IllegalArgumentException(e);
						}
					}
				};
			}

			@Override
			public Function<T, Buffer> encoder() {
				return new Function<T, Buffer>() {
					@Override
					public Buffer apply(T t) {
						try {
							return Buffer.wrap(mapper.writeValueAsBytes(t));
						} catch(JsonProcessingException e) {
							throw new IllegalArgumentException(e);
						}
					}
				};
			}
		};
	}

}
