package reactor.data.spring.redis;

import com.lambdaworks.redis.RedisClient;
import org.aopalliance.aop.Advice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import reactor.core.Environment;
import reactor.data.core.ComposableMessagingRepository;
import reactor.data.redis.RedisComposableMessagingRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.core.GenericTypeResolver.resolveTypeArguments;

/**
 * @author Jon Brisbin
 */
public class RedisComposableMessagingRepositoryPostProcessor<V>
		extends AbstractComposableRepositoryPostProcessor<ComposableMessagingRepository<V, String>>
		implements BeanFactoryAware {

	private final ReentrantLock repoLock = new ReentrantLock();
	private final long                                     timeout;
	private       ListableBeanFactory                      beanFactory;
	private       ComposableMessagingRepository<V, String> composableRepo;

	public RedisComposableMessagingRepositoryPostProcessor(Environment env,
	                                                       String dispatcher,
	                                                       Executor executor,
	                                                       String[] basePackages) {
		super(env, dispatcher, executor, basePackages);
		this.timeout = env != null ? env.getProperty("reactor.await.defaultTimeout", Long.class, 60000L) : 60000L;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if(beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory)beanFactory;
		}
	}

	@Override
	protected Class<?> getRepositoryType() {
		return ComposableMessagingRepository.class;
	}

	@Override
	protected ComposableMessagingRepository<V, String> getRepositoryProxy(Class<ComposableMessagingRepository<V,
			String>> repoType) {
		repoLock.lock();
		try {
			if(null != composableRepo) {
				return composableRepo;
			}

			Class<V> managedType = null;
			for(Class<?> intfType : repoType.getInterfaces()) {
				if(!ComposableMessagingRepository.class.isAssignableFrom(intfType)) {
					continue;
				}
				Class<?>[] types = resolveTypeArguments(repoType, ComposableMessagingRepository.class);
				managedType = (Class<V>)types[0];
				break;
			}

			if(null == managedType) {
				return null;
			}

			RedisClient client = beanFactory.getBean(RedisClient.class);
			return composableRepo = new RedisComposableMessagingRepository<>(getEnvironment(),
			                                                                 getDispatcher(),
			                                                                 getExecutor(),
			                                                                 client,
			                                                                 timeout,
			                                                                 managedType);
		} finally {
			repoLock.unlock();
		}
	}

	@Override
	protected List<Advice> getAdvice(Class<ComposableMessagingRepository<V, String>> repoType,
	                                 ComposableMessagingRepository<V, String> obj) {
		return null;
	}

}
