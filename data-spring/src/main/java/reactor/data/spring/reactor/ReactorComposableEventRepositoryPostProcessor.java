package reactor.data.spring.reactor;

import org.aopalliance.aop.Advice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.StringUtils;
import reactor.core.Environment;
import reactor.data.core.ComposableEventRepository;
import reactor.data.core.annotation.Provider;
import reactor.data.reactor.ReactorComposableEventRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.core.GenericTypeResolver.resolveTypeArguments;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author Jon Brisbin
 */
public class ReactorComposableEventRepositoryPostProcessor<T, K extends Serializable>
		extends AbstractComposableRepositoryPostProcessor<ComposableEventRepository<T, K>>
		implements BeanFactoryAware {

	private final ReentrantLock repoLock = new ReentrantLock();
	private ListableBeanFactory             beanFactory;
	private ComposableEventRepository<T, K> composableRepo;

	public ReactorComposableEventRepositoryPostProcessor(Environment env,
	                                                     String dispatcher,
	                                                     Executor executor,
	                                                     String[] basePackages) {
		super(env, dispatcher, executor, basePackages);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if(beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory)beanFactory;
		}
	}

	@Override
	protected Class<?> getRepositoryType() {
		return ComposableEventRepository.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ComposableEventRepository<T, K> getRepositoryProxy(Class<ComposableEventRepository<T, K>> repoType) {
		repoLock.lock();
		try {
			if(null != composableRepo) {
				return composableRepo;
			}

			Class<T> managedType = null;
			for(Class<?> intfType : repoType.getInterfaces()) {
				if(!ComposableEventRepository.class.isAssignableFrom(intfType)) {
					continue;
				}
				Provider providerAnno = findAnnotation(intfType, Provider.class);
				if(null != providerAnno && StringUtils.hasText(providerAnno.value())) {
					if(!"reactor".equals(providerAnno.value())) {
						continue;
					}
				}

				Class<?>[] types = resolveTypeArguments(repoType, ComposableEventRepository.class);
				managedType = (Class<T>)types[0];
				break;
			}

			if(null == managedType) {
				return null;
			}

			return composableRepo = new ReactorComposableEventRepository<>(getEnvironment(), getDispatcher());
		} finally {
			repoLock.unlock();
		}
	}

	@Override
	protected List<Advice> getAdvice(Class<ComposableEventRepository<T, K>> repoType,
	                                 ComposableEventRepository<T, K> obj) {
		return null;
	}

}
