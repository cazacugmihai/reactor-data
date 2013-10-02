package reactor.data.spring.redis;

import com.lambdaworks.redis.RedisClient;
import org.aopalliance.aop.Advice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import reactor.core.Environment;
import reactor.data.core.ComposableCounterRepository;
import reactor.data.redis.RedisComposableCounterRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Jon Brisbin
 */
public class RedisComposableCounterRepositoryPostProcessor
		extends AbstractComposableRepositoryPostProcessor<ComposableCounterRepository>
		implements BeanFactoryAware {

	private final ReentrantLock repoLock = new ReentrantLock();
	private BeanFactory                 beanFactory;
	private ComposableCounterRepository composableRepo;

	public RedisComposableCounterRepositoryPostProcessor(Environment env,
	                                                     String dispatcher,
	                                                     Executor executor,
	                                                     String[] basePackages) {
		super(env, dispatcher, executor, basePackages);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	protected Class<?> getRepositoryType() {
		return ComposableCounterRepository.class;
	}

	@Override
	protected ComposableCounterRepository getRepositoryProxy(Class<ComposableCounterRepository> repoType) {
		repoLock.lock();
		try {
			if(null != composableRepo) {
				return composableRepo;
			}

			RedisClient client = beanFactory.getBean(RedisClient.class);
			return composableRepo = new RedisComposableCounterRepository(
					getEnvironment(),
					getDispatcher(),
					client.connect(new CounterCodec()),
					getExecutor()
			);
		} finally {
			repoLock.unlock();
		}
	}

	@Override
	protected List<Advice> getAdvice(Class<ComposableCounterRepository> repoType, ComposableCounterRepository obj) {
		return null;
	}

}
