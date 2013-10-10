package reactor.data.spring.redis;

import com.lambdaworks.redis.RedisClient;
import reactor.core.Environment;
import reactor.data.core.ComposableCacheRepository;
import reactor.data.redis.RedisComposableCacheRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 */
public class RedisComposableCacheRepositoryPostProcessor<V>
		extends AbstractComposableRepositoryPostProcessor<V, ComposableCacheRepository<V, String>> {

	private final long timeout;

	public RedisComposableCacheRepositoryPostProcessor(Environment env,
	                                                   String dispatcher,
	                                                   Executor executor,
	                                                   String[] basePackages) {
		super(env, dispatcher, executor, basePackages);
		this.timeout = env != null ? env.getProperty("reactor.await.defaultTimeout", Long.class, 60000L) : 60000L;
	}

	@Override
	protected Class<?> getRepositoryType() {
		return ComposableCacheRepository.class;
	}

	@Override
	protected String getProviderName() {
		return "redis";
	}

	@Override
	protected ComposableCacheRepository<V, String> createRepositoryProxy(Class<V> managedType) {
		RedisClient client = beanFactory.getBean(RedisClient.class);
		return new RedisComposableCacheRepository<>(getEnvironment(),
		                                            getDispatcher(),
		                                            getExecutor(),
		                                            client,
		                                            timeout,
		                                            managedType);
	}

}
