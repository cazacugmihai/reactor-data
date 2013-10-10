package reactor.data.spring.redis;

import com.lambdaworks.redis.RedisClient;
import reactor.core.Environment;
import reactor.data.core.ComposableMessagingRepository;
import reactor.data.redis.RedisComposableMessagingRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 */
public class RedisComposableMessagingRepositoryPostProcessor<V>
		extends AbstractComposableRepositoryPostProcessor<V, ComposableMessagingRepository<V, String>> {

	private final long timeout;

	public RedisComposableMessagingRepositoryPostProcessor(Environment env,
	                                                       String dispatcher,
	                                                       Executor executor,
	                                                       String[] basePackages) {
		super(env, dispatcher, executor, basePackages);
		this.timeout = env != null ? env.getProperty("reactor.await.defaultTimeout", Long.class, 60000L) : 60000L;
	}

	@Override
	protected Class<?> getRepositoryType() {
		return ComposableMessagingRepository.class;
	}

	@Override
	protected String getProviderName() {
		return "redis";
	}

	@Override
	protected ComposableMessagingRepository<V, String> createRepositoryProxy(Class<V> managedType) {
		return new RedisComposableMessagingRepository<>(getEnvironment(),
		                                                getDispatcher(),
		                                                getExecutor(),
		                                                beanFactory.getBean(RedisClient.class),
		                                                timeout,
		                                                managedType);
	}

}
