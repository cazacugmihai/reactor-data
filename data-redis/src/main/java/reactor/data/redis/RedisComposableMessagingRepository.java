package reactor.data.redis;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.pubsub.RedisPubSubAdapter;
import com.lambdaworks.redis.pubsub.RedisPubSubConnection;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.spec.Reactors;
import reactor.data.core.ComposableMessagingRepository;
import reactor.data.redis.codec.ObjectMapperCodec;
import reactor.event.Event;
import reactor.event.support.EventConsumer;
import reactor.util.Assert;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static reactor.event.selector.Selectors.$;

/**
 * @author Jon Brisbin
 */
public class RedisComposableMessagingRepository<V>
		extends AbstractRedisComposableRepository
		implements ComposableMessagingRepository<V, String> {

	private static final String TYPE = "messaging";

	private final RedisPubSubConnection<String, V> redis;
	private final Class<V>                         type;
	private final String                           typeName;
	private final Reactor                          reactor;

	public RedisComposableMessagingRepository(Environment env,
	                                          String dispatcher,
	                                          Executor executor,
	                                          RedisClient client,
	                                          long timeout,
	                                          Class<V> type) {
		super(env, dispatcher, executor, client, timeout);
		this.redis = client.connectPubSub(new ObjectMapperCodec<>(type));
		this.type = type;
		this.typeName = type.getName();
		this.reactor = Reactors.reactor().env(env).dispatcher(dispatcher).get();

		redis.addListener(new RedisPubSubAdapter<String, V>() {
			@Override
			public void message(String channel, V message) {
				reactor.notify(formatKey(TYPE, typeName, channel), Event.wrap(message));
			}
		});
	}

	@Override
	public Promise<Void> send(final String key, final V message) {
		Assert.notNull(key, "Key cannot be null");

		final Deferred<Void, Promise<Void>> d = createDeferredPromise();

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					redis.publish(formatKey(TYPE, typeName, key), message).get(getTimeout(), TimeUnit.MILLISECONDS);
					d.accept((Void)null);
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});

		return d.compose();
	}

	@Override
	public Stream<V> receive(String key) {
		Assert.notNull(key, "Key cannot be null");

		Deferred<V, Stream<V>> d = createDeferredStream();

		reactor.on($(formatKey(TYPE, typeName, key)), new EventConsumer<>(d));

		return d.compose();
	}

}
