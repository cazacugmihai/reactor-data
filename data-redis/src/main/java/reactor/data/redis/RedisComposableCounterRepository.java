package reactor.data.redis;

import com.lambdaworks.redis.RedisConnection;
import reactor.core.Environment;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.spec.DeferredPromiseSpec;
import reactor.core.composable.spec.Promises;
import reactor.data.core.ComposableCounterRepository;
import reactor.function.Consumer;
import reactor.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 */
public class RedisComposableCounterRepository implements ComposableCounterRepository {

	private final Map<String, Consumer<Long>> incrementers = new ConcurrentHashMap<>();
	private final Environment                   env;
	private final String                        dispatcher;
	private final RedisConnection<String, Long> redis;
	private final Executor                      executor;

	public RedisComposableCounterRepository(Environment env,
	                                        String dispatcher,
	                                        RedisConnection<String, Long> conn,
	                                        Executor executor) {
		Assert.notNull(conn, "RedisConnection cannot be null.");
		this.env = env;
		this.dispatcher = dispatcher;
		this.redis = conn;
		this.executor = executor;
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
					executor.execute(new Runnable() {
						@Override
						public void run() {
							if(l < 0) {
								redis.decrby(name, Math.abs(l));
							} else {
								redis.incrby(name, l);
							}
						}
					});
				}
			};
			incrementers.put(name, c);
		}
		return c;
	}

	@Override
	public Promise<Long> get(final String name) {
		final Deferred<Long, Promise<Long>> d = createDeferred();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				d.accept(redis.get(name));
			}
		});
		return d.compose();
	}

	@Override
	public Promise<Long> remove(final String name) {
		final Deferred<Long, Promise<Long>> d = createDeferred();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				d.accept(redis.del(name));
			}
		});
		return d.compose();
	}

	private Deferred<Long, Promise<Long>> createDeferred() {
		DeferredPromiseSpec<Long> promiseSpec = Promises.<Long>defer().env(env);
		if(null == dispatcher) {
			promiseSpec.synchronousDispatcher();
		} else {
			promiseSpec.dispatcher(dispatcher);
		}
		return promiseSpec.get();
	}

}
