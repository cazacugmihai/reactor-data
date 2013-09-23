package reactor.data.core;

import reactor.core.composable.Promise;

import java.io.Serializable;

/**
 * @author Jon Brisbin
 */
public interface ComposableCacheRepository<V, K extends Serializable> {

	Promise<V> get(K key);

	Promise<V> set(K key);

	Promise<V> remove(K key);

}
