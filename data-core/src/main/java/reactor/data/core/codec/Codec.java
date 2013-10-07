package reactor.data.core.codec;

import reactor.function.Consumer;
import reactor.function.Function;

/**
 * @author Jon Brisbin
 */
public interface Codec<IN, OUT, V> {

	Consumer<IN> decoder(Consumer<V> next);

	Function<V, OUT> encoder();

}
