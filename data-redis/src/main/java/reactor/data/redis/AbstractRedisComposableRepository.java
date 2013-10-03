package reactor.data.redis;

import com.lambdaworks.redis.RedisClient;
import reactor.core.Environment;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.DeferredPromiseSpec;
import reactor.core.composable.spec.DeferredStreamSpec;
import reactor.core.composable.spec.Promises;
import reactor.core.composable.spec.Streams;

import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 */
abstract class AbstractRedisComposableRepository {

	static final String PREFIX = "reactor.composable:";

	private final Environment env;
	private final String      dispatcher;
	private final Executor    executor;
	private final RedisClient client;
	private final long        timeout;


	protected AbstractRedisComposableRepository(Environment env,
	                                            String dispatcher,
	                                            Executor executor,
	                                            RedisClient client,
	                                            long timeout) {
		this.env = env;
		this.dispatcher = dispatcher;
		this.executor = executor;
		this.client = client;
		this.timeout = timeout;
	}

	public Environment getEnvironment() {
		return env;
	}

	public String getDispatcher() {
		return dispatcher;
	}

	public Executor getExecutor() {
		return executor;
	}

	public RedisClient getClient() {
		return client;
	}

	public long getTimeout() {
		return timeout;
	}

	protected <T> Deferred<T, Promise<T>> createDeferredPromise() {
		DeferredPromiseSpec<T> promiseSpec = Promises.<T>defer().env(env);
		if(null != dispatcher) {
			promiseSpec.dispatcher(dispatcher);
		}
		return promiseSpec.get();
	}

	protected <T> Deferred<T, Stream<T>> createDeferredStream() {
		DeferredStreamSpec<T> streamSpec = Streams.<T>defer().env(env);
		if(null != dispatcher) {
			streamSpec.dispatcher(dispatcher);
		}
		return streamSpec.get();
	}

	protected String unformatKey(String type, String name, String key) {
		return key.replaceAll(formatKey(type, name, ""), "");
	}

	protected String formatKey(String type, String name, String key) {
		return String.format("reactor.composable.%s:%s:%s", type, name, key);
	}

}
