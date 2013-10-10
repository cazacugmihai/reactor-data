package reactor.data.spring.redis;

import com.lambdaworks.redis.RedisClient;
import reactor.core.Environment;
import reactor.data.core.ComposableCounterRepository;
import reactor.data.redis.RedisComposableCounterRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 */
public class RedisComposableCounterRepositoryPostProcessor
		extends AbstractComposableRepositoryPostProcessor<Long, ComposableCounterRepository> {

	private final long timeout;

	public RedisComposableCounterRepositoryPostProcessor(Environment env,
	                                                     String dispatcher,
	                                                     Executor executor,
	                                                     String[] basePackages) {
		super(env, dispatcher, executor, basePackages);
		this.timeout = env != null ? env.getProperty("reactor.await.defaultTimeout", Long.class, 60000L) : 60000L;
	}

	@Override
	protected Class<?> getRepositoryType() {
		return ComposableCounterRepository.class;
	}

	@Override
	protected String getProviderName() {
		return "redis";
	}

	@Override
	protected ComposableCounterRepository createRepositoryProxy(Class<Long> managedType) {
		return new RedisComposableCounterRepository(getEnvironment(),
		                                            getDispatcher(),
		                                            getExecutor(),
		                                            beanFactory.getBean(RedisClient.class),
		                                            timeout,
		                                            Long.class.getName());
	}

}
