package reactor.data.redis;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import reactor.core.Environment;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.data.core.ComposableCacheRepository;
import reactor.data.redis.codec.ObjectMapperCodec;
import reactor.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 */
public class RedisComposableCacheRepository<V>
		extends AbstractRedisComposableRepository
		implements ComposableCacheRepository<V, String> {

	private static final String TYPE = "cache";

	private final RedisConnection<String, V> redis;
	private final Class<V>                   type;
	private final String                     typeName;

	public RedisComposableCacheRepository(Environment env,
	                                      String dispatcher,
	                                      Executor executor,
	                                      RedisClient client,
	                                      long timeout,
	                                      Class<V> type) {
		super(env, dispatcher, executor, client, timeout);
		Assert.notNull(client, "RedisClient cannot be null.");
		this.redis = client.connect(new ObjectMapperCodec<>(type));
		this.type = type;
		this.typeName = type.getName();
	}

	@Override
	public Promise<List<String>> keys() {
		final Deferred<List<String>, Promise<List<String>>> d = createDeferredPromise();

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					List<String> keys = redis.keys(formatKey(TYPE, typeName, "*"));
					List<String> subkeys = new ArrayList<>(keys.size());
					for(String key : keys) {
						subkeys.add(unformatKey(TYPE, typeName, key));
					}
					d.accept(subkeys);
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});

		return d.compose();
	}

	@Override
	public Promise<V> get(final String key) {
		Assert.notNull(key, "Key cannot be null");

		final Deferred<V, Promise<V>> d = createDeferredPromise();

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					d.accept(redis.get(formatKey(TYPE, typeName, key)));
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});

		return d.compose();
	}

	@Override
	public Promise<V> set(final String key, final V value) {
		Assert.notNull(key, "Key cannot be null");
		Assert.notNull(key, "Value cannot be null");

		final Deferred<V, Promise<V>> d = createDeferredPromise();

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					d.accept(redis.getset(formatKey(TYPE, typeName, key), value));
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});

		return d.compose();
	}

	@Override
	public Promise<V> set(final String key,
	                      final V value,
	                      final long ttl,
	                      final TimeUnit timeUnit) {
		Assert.notNull(key, "Key cannot be null");
		Assert.notNull(key, "Value cannot be null");

		final Deferred<V, Promise<V>> d = createDeferredPromise();

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				String k = formatKey(TYPE, typeName, key);
				try {
					long seconds = TimeUnit.SECONDS.convert(ttl, timeUnit);
					V v = redis.getset(k, value);
					redis.expire(k, seconds);
					d.accept(v);
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});

		return d.compose();
	}

	@Override
	public Promise<V> remove(final String key) {
		Assert.notNull(key, "Key cannot be null");

		final Deferred<V, Promise<V>> d = createDeferredPromise();

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					String k = formatKey(TYPE, typeName, key);
					V value = redis.get(k);
					redis.del(k);
					d.accept(value);
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});

		return d.compose();
	}

}
