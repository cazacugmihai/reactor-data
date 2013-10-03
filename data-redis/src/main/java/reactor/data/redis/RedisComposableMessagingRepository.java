package reactor.data.redis;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.pubsub.RedisPubSubConnection;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.spec.Reactors;
import reactor.data.core.ComposableMessagingRepository;
import reactor.data.redis.codec.ObjectMapperCodec;
import reactor.event.Event;
import reactor.event.registry.Registration;
import reactor.event.support.EventConsumer;
import reactor.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static reactor.event.selector.Selectors.$;
import static reactor.event.selector.Selectors.R;

/**
 * @author Jon Brisbin
 */
public class RedisComposableMessagingRepository<V>
		extends AbstractRedisComposableRepository
		implements ComposableMessagingRepository<V, String> {

	private static final Logger LOG  = LoggerFactory.getLogger(RedisComposableMessagingRepository.class);
	private static final String TYPE = "messaging";

	private final Map<String, Deferred<V, Stream<V>>> deferreds     = new ConcurrentHashMap<>();
	private final Map<String, Registration>           registrations = new ConcurrentHashMap<>();
	private final RedisConnection<String, V>       pub;
	private final RedisPubSubConnection<String, V> sub;
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
		ObjectMapperCodec<V> codec = new ObjectMapperCodec<>(type);
		this.pub = client.connect(codec);
		this.sub = client.connectPubSub(codec);
		this.type = type;
		this.typeName = type.getName();
		this.reactor = Reactors.reactor().env(env).synchronousDispatcher().get();

		sub.addListener(new RedisPubSubListener<String, V>() {
			@Override
			public void message(String channel, V message) {
				//LOG.info("message: {} {}", channel, message);
				message(null, channel, message);
			}

			@Override
			public void message(String pattern, String channel, V message) {
				//LOG.info("message: {} {} {}", pattern, channel, message);
				reactor.notify(channel, Event.wrap(message));
			}

			@Override
			public void subscribed(String channel, long count) {
				//LOG.info("subscribed: {} {}", channel, count);
				Deferred<V, Stream<V>> deferred = deferreds.remove(channel);
				if(null == deferred) {
					return;
				}
				registrations.put(channel, reactor.on($(channel), new EventConsumer<>(deferred)));
			}

			@Override
			public void psubscribed(String pattern, long count) {
				//LOG.info("psubscribed: {} {}", pattern, count);
				Deferred<V, Stream<V>> deferred = deferreds.remove(pattern);
				if(null == deferred) {
					return;
				}
				if(pattern.contains("*")) {
					pattern = pattern.replaceAll("\\*", "(.*)");
				}
				registrations.put(pattern, reactor.on(R(pattern), new EventConsumer<>(deferred)));
			}

			@Override
			public void unsubscribed(String channel, long count) {
				//LOG.info("unsubscribed: {} {}", channel, count);
				Registration reg = registrations.remove(channel);
				if(null != reg) {
					reg.cancel();
				}
			}

			@Override
			public void punsubscribed(String pattern, long count) {
				//LOG.info("punsubscribed: {} {}", pattern, count);
				Registration reg = registrations.remove(pattern);
				if(null != reg) {
					reg.cancel();
				}
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
					LOG.info("publishing: {} to {}", message, formatKey(TYPE, typeName, key));
					pub.publish(formatKey(TYPE, typeName, key), message);
					d.accept((Void)null);
				} catch(Exception e) {
					d.accept(e);
				}
			}
		});

		return d.compose();
	}

	@Override
	public Stream<V> receive(final String key) {
		Assert.notNull(key, "Key cannot be null");

		Deferred<V, Stream<V>> d = createDeferredStream();

		final String channel = formatKey(TYPE, typeName, key);
		LOG.info("subscribing to {}", channel);
		deferreds.put(channel, d);

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				if(key.contains("*")) {
					sub.psubscribe(channel);
				} else {
					sub.subscribe(channel);
				}
			}
		});

		return d.compose();
	}

}
