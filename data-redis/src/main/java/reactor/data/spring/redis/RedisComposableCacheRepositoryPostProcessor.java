package reactor.data.spring.redis;

import com.lambdaworks.redis.RedisClient;
import org.aopalliance.aop.Advice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.StringUtils;
import reactor.core.Environment;
import reactor.data.core.ComposableCacheRepository;
import reactor.data.core.annotation.Provider;
import reactor.data.redis.RedisComposableCacheRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.core.GenericTypeResolver.resolveTypeArguments;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author Jon Brisbin
 */
public class RedisComposableCacheRepositoryPostProcessor<V>
		extends AbstractComposableRepositoryPostProcessor<ComposableCacheRepository<V, String>>
		implements BeanFactoryAware {

	private final ReentrantLock repoLock = new ReentrantLock();
	private final long                                 timeout;
	private       ListableBeanFactory                  beanFactory;
	private       ComposableCacheRepository<V, String> composableRepo;

	public RedisComposableCacheRepositoryPostProcessor(Environment env,
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
		return ComposableCacheRepository.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ComposableCacheRepository<V, String> getRepositoryProxy(Class<ComposableCacheRepository<V, String>> type) {
		repoLock.lock();
		try {
			if(null != composableRepo) {
				return composableRepo;
			}

			Class<V> managedType = null;
			for(Class<?> intfType : type.getInterfaces()) {
				if(!ComposableCacheRepository.class.isAssignableFrom(intfType)) {
					continue;
				}
				Provider providerAnno = findAnnotation(intfType, Provider.class);
				if(null != providerAnno && StringUtils.hasText(providerAnno.value())) {
					if(!"redis".equals(providerAnno.value())) {
						continue;
					}
				}
				Class<?>[] types = resolveTypeArguments(type, ComposableCacheRepository.class);
				managedType = (Class<V>)types[0];
				break;
			}

			if(null == managedType) {
				return null;
			}

			RedisClient client = beanFactory.getBean(RedisClient.class);
			return composableRepo = new RedisComposableCacheRepository<>(getEnvironment(),
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
	protected List<Advice> getAdvice(Class<ComposableCacheRepository<V, String>> repoType,
	                                 ComposableCacheRepository<V, String> obj) {
		return null;
	}

}
