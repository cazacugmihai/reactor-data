package reactor.data.spring;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;
import reactor.core.Environment;
import reactor.data.core.ComposableRepository;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 */
public abstract class AbstractComposableRepositoryPostProcessor<T>
		implements BeanDefinitionRegistryPostProcessor,
		           ResourceLoaderAware {

	private final Environment env;
	private final String      dispatcher;
	private final Executor    executor;

	private final String[]       basePackages;
	private       ResourceLoader resourceLoader;

	protected AbstractComposableRepositoryPostProcessor(
			Environment env,
			String dispatcher,
			Executor executor,
			String[] basePackages
	) {
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

				Class<T> repoType;
				try {
					repoType = (Class<T>)Class.forName(beanDef.getBeanClassName());
				} catch(ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}

				T repo = getRepositoryProxy(repoType);

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

	protected abstract Class<?> getRepositoryType();

	protected abstract T getRepositoryProxy(Class<T> repoType);

	protected abstract List<Advice> getAdvice(Class<T> repoType, T obj);

}
