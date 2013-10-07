package reactor.data.core;

import reactor.core.composable.Promise;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@code ComposableCacheRepository} is a simple K/V store that uses some kind of simple serialization to store the
 * {@code K} keys and {@code V} values. Each type of a {@code ComposableCacheRepository} has its own keyspace for cache
 * keys. For example, if a cache for {@code Person} objects is needed, create an interface named {@code PersonCache}
 * that extends from {@code ComposableCacheRepository} and manages type {@code Person}.
 * <p>
 * <pre><code>
 *     public interface PersonCache extends ComposableCacheRepository&lt;Person,String&gt; {}
 *   </code></pre>
 * </p>
 *
 * @author Jon Brisbin
 */
public interface ComposableCacheRepository<V, K extends Serializable> extends ComposableRepository<V, K> {

	/**
	 * Get a list of the keys this cache repository knows about and fulfill the returned {@link
	 * reactor.core.composable.Promise}.
	 *
	 * @return a {@link reactor.core.composable.Promise} that will be fulfilled with the of a search for keys.
	 */
	Promise<List<K>> keys();

	/**
	 * Get a cache entry for the given key. Might return {@code null} if no cache entry exists.
	 *
	 * @param key
	 * 		the cache key to look for.
	 *
	 * @return a {@link reactor.core.composable.Promise} that will be fulfilled with the value retrieved from the
	 * cache or {@code null} if no value was found.
	 */
	Promise<V> get(K key);

	/**
	 * Set a cache entry for the given value.
	 *
	 * @param key
	 * 		The cache key to use. Cannot be {@code null}.
	 * @param value
	 * 		The value to cache. Cannot be {@code null}.
	 *
	 * @return a {@link reactor.core.composable.Promise} that will be fulfilled with the previous value of the cache for
	 * that key (if one existed) before being overwritten with the new value.
	 */
	Promise<V> set(K key, V value);

	/**
	 * Similar to {@link #set(java.io.Serializable, Object)} except expire the cache entry after the given timeout/ttl.
	 *
	 * @param key
	 * 		The cache key to use. Cannot be {@code null}.
	 * @param value
	 * 		The value to cache. Cannot be {@code null}.
	 * @param ttl
	 * 		The TTL (time to live) for this cache entry.
	 * @param timeUnit
	 * 		The unit of time the TTL value is measured in.
	 *
	 * @return a {@link reactor.core.composable.Promise} that will be fulfilled with the previous value of the cache for
	 * that key (if one existed) before being overwritten with the new value.
	 */
	Promise<V> set(K key, V value, long ttl, TimeUnit timeUnit);

	/**
	 * Remove a cache entry.
	 *
	 * @param key
	 * 		The cache key to use. Cannot be {@code null}.
	 *
	 * @return a {@link reactor.core.composable.Promise} that will be fulfilled with the previous value of the cache for
	 * that key (if one existed) before being removed.
	 */
	Promise<V> remove(K key);

}
