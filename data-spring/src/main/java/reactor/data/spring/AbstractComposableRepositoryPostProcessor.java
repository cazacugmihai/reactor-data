package reactor.data.spring;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.StringUtils;
import reactor.core.Environment;
import reactor.data.core.ComposableRepository;
import reactor.data.core.annotation.Provider;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.core.GenericTypeResolver.resolveTypeArguments;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author Jon Brisbin
 */
public abstract class
		AbstractComposableRepositoryPostProcessor<V, R extends ComposableRepository<V, ? extends Serializable>>
		implements BeanDefinitionRegistryPostProcessor,
		           ResourceLoaderAware,
		           BeanFactoryAware {

	private final ReentrantLock repoLock = new ReentrantLock();

	private final Environment env;
	private final String      dispatcher;
	private final Executor    executor;
	private final String[]    basePackages;

	protected ResourceLoader      resourceLoader;
	protected ListableBeanFactory beanFactory;
	private   R                   composableRepo;

	protected AbstractComposableRepositoryPostProcessor(Environment env,
	                                                    String dispatcher,
	                                                    Executor executor,
	                                                    String[] basePackages) {
		this.env = env;
		this.dispatcher = dispatcher;
		this.executor = executor;
		this.basePackages = basePackages;
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

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory)beanFactory;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				return beanDefinition.getMetadata().isIndependent();
			}
		};
		scanner.addIncludeFilter(new AssignableTypeFilter(getRepositoryType()));
		scanner.setResourceLoader(resourceLoader);

		for(String basePackage : basePackages) {
			for(BeanDefinition beanDef : scanner.findCandidateComponents(basePackage)) {
				if(beanFactory.containsBean(beanDef.getBeanClassName())) {
					continue;
				}

				Class<R> repoType;
				try {
					repoType = (Class<R>)Class.forName(beanDef.getBeanClassName());
				} catch(ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}

				R repo = getRepositoryProxy(repoType);
				if(null == repo) {
					continue;
				}

				ProxyFactory pf = new ProxyFactory(repo);
				pf.addInterface(repoType);
				pf.addInterface(ComposableRepository.class);

				List<Advice> advice = getAdvice(repoType, repo);
				if(null != advice) {
					for(Advice ad : advice) {
						pf.addAdvice(ad);
					}
				}

				beanFactory.registerSingleton(beanDef.getBeanClassName(), pf.getProxy());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private R getRepositoryProxy(Class<R> repoType) {
		repoLock.lock();
		try {
			if(null != composableRepo) {
				return composableRepo;
			}

			Class<V> managedType = null;
			for(Class<?> intfType : repoType.getInterfaces()) {
				if(!getRepositoryType().isAssignableFrom(intfType)) {
					continue;
				}
				Provider providerAnno = findAnnotation(intfType, Provider.class);
				if(null != providerAnno && StringUtils.hasText(providerAnno.value())) {
					if(!getProviderName().equals(providerAnno.value())) {
						continue;
					}
				}
				Class<?>[] types = resolveTypeArguments(repoType, getRepositoryType());
				if(null == types) {
					continue;
				}
				managedType = (Class<V>)types[0];
				break;
			}

			return composableRepo = createRepositoryProxy(managedType);
		} finally {
			repoLock.unlock();
		}
	}

	protected List<Advice> getAdvice(Class<R> repoType, R composableRepo) {
		return null;
	}

	protected abstract Class<?> getRepositoryType();

	protected abstract String getProviderName();

	protected abstract R createRepositoryProxy(Class<V> managedType);

}
