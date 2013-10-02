package reactor.data.core;

import reactor.core.composable.Promise;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 */
public interface ComposableCacheRepository<V, K extends Serializable> {

	Promise<List<K>> keys();

	Promise<V> get(K key);

	Promise<V> set(K key, V value);

	Promise<V> set(K key, V value, long ttl, TimeUnit timeUnit);

	Promise<V> remove(K key);

}
