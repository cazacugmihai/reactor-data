package reactor.data.redis;

import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisClient;
import reactor.core.Environment;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.data.core.ComposableCounterRepository;
import reactor.data.redis.codec.CounterCodec;
import reactor.function.Consumer;
import reactor.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 */
public class RedisComposableCounterRepository
		extends AbstractRedisComposableRepository
		implements ComposableCounterRepository {

	private static final String TYPE = "counter";

	private final Map<String, Consumer<Long>> incrementers = new ConcurrentHashMap<>();
	private final RedisAsyncConnection<String, Long> redis;
	private final String                             cacheName;

	public RedisComposableCounterRepository(Environment env,
	                                        String dispatcher,
	                                        Executor executor,
	                                        RedisClient client,
	                                        long timeout,
	                                        String cacheName) {
		super(env, dispatcher, executor, client, timeout);
		Assert.notNull(client, "RedisClient cannot be null.");
		this.redis = client.connectAsync(new CounterCodec());
		this.cacheName = cacheName;
	}

	@Override
	public ComposableCounterRepository decr(String name) {
		add(name).accept(-1L);
		return this;
	}

	@Override
	public ComposableCounterRepository incr(String name) {
		add(name).accept(1L);
		return this;
	}

	@Override
	public Consumer<Long> add(final String name) {
		Consumer<Long> c;
		if(null == (c = incrementers.get(name))) {
			c = new Consumer<Long>() {
				@Override
				public void accept(final Long l) {
					String k = formatKey(TYPE, cacheName, name);
					if(l < 0) {
						redis.decrby(k, Math.abs(l));
					} else {
						redis.incrby(k, l);
					}
				}
			};
			incrementers.put(name, c);
		}
		return c;
	}

	@Override
	public Promise<Long> get(final String name) {
		final Deferred<Long, Promise<Long>> d = createDeferredPromise();
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				String k = formatKey(TYPE, cacheName, name);
				try {
					d.accept(redis.get(k).get(getTimeout(), TimeUnit.MILLISECONDS));
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});
		return d.compose();
	}

	@Override
	public Promise<Long> remove(final String name) {
		final Deferred<Long, Promise<Long>> d = createDeferredPromise();
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				String k = formatKey(TYPE, cacheName, name);
				try {
					d.accept(redis.del(k).get(getTimeout(), TimeUnit.MILLISECONDS));
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});
		return d.compose();
	}

}
