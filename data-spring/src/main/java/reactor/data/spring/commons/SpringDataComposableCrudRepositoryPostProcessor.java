package reactor.data.spring.commons;

import org.aopalliance.aop.Advice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.repository.support.Repositories;
import reactor.core.Environment;
import reactor.data.core.ComposableCrudRepository;
import reactor.data.core.ComposableRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.core.GenericTypeResolver.resolveTypeArguments;

/**
 * @author Jon Brisbin
 */
public class SpringDataComposableCrudRepositoryPostProcessor<T, K extends Serializable>
		extends AbstractComposableRepositoryPostProcessor<ComposableCrudRepository<T, K>>
		implements BeanFactoryAware {

	private final ReentrantLock repoLock = new ReentrantLock();
	private ListableBeanFactory            beanFactory;
	private Repositories                   repositories;
	private ComposableCrudRepository<T, K> composableRepo;


	public SpringDataComposableCrudRepositoryPostProcessor(
			Environment env,
			String dispatcher,
			Executor executor,
			String[] basePackages
	) {
		super(env, dispatcher, executor, basePackages);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if(beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory)beanFactory;
			this.repositories = new Repositories((ListableBeanFactory)beanFactory);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Class<?> getRepositoryType() {
		return ComposableCrudRepository.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ComposableCrudRepository<T, K> getRepositoryProxy(Class<ComposableCrudRepository<T, K>> repoType) {
		repoLock.lock();
		try {
			if(null != composableRepo) {
				return composableRepo;
			}

			Class<?> managedType = null;
			for(Class<?> intfType : repoType.getInterfaces()) {
				if(!ComposableRepository.class.isAssignableFrom(intfType)) {
					continue;
				}
				Class<?>[] types = resolveTypeArguments(repoType, ComposableRepository.class);
				managedType = types[0];
				break;
			}

			if(null == managedType) {
				return null;
			}

			return (composableRepo = new SpringDataComposableCrudRepository<>(getEnvironment(),
			                                                                  getDispatcher(),
			                                                                  getExecutor(),
			                                                                  repositories,
			                                                                  managedType));
		} finally {
			repoLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Advice> getAdvice(
			Class<ComposableCrudRepository<T, K>> repoType,
			ComposableCrudRepository<T, K> repo
	) {
		return Arrays.<Advice>asList(new SpringDataRepositoryQueryMethodMethodInterceptor<>(getEnvironment(),
		                                                                                    getDispatcher(),
		                                                                                    getExecutor(),
		                                                                                    repoType,
		                                                                                    (SpringDataComposableCrudRepository<Object, Serializable>)repo));
	}


}
