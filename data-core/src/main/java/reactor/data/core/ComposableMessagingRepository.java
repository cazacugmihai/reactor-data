package reactor.data.core;

import reactor.core.composable.Promise;
import reactor.core.composable.Stream;

import java.io.Serializable;

/**
 * @author Jon Brisbin
 */
public interface ComposableMessagingRepository<V, K extends Serializable> {

	Promise<Void> send(K key, V message);

	Stream<V> receive(K key);

}
