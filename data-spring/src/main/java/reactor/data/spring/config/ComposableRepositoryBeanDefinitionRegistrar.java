package reactor.data.spring.config;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import reactor.core.Environment;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;
import reactor.queue.BlockingQueueFactory;
import reactor.support.NamedDaemonThreadFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 */
public class ComposableRepositoryBeanDefinitionRegistrar
		implements ImportBeanDefinitionRegistrar,
		           ResourceLoaderAware {

	public static final String REACTOR_ENV   = "reactorEnv";
	public static final String EXECUTOR_BEAN = "reactorIOExecutor";

	private final ClassLoader classLoader = getClass().getClassLoader();
	private ResourceLoader resourceLoader;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {

		Map<String, Object> attrs = meta.getAnnotationAttributes(EnableComposableRepositories.class.getName());

		ClassPathScanningCandidateComponentProvider postProcessors =
				new ClassPathScanningCandidateComponentProvider(false) {
					@Override
					protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
						return beanDefinition.getMetadata().isIndependent();
					}
				};
		postProcessors.addIncludeFilter(
				new AssignableTypeFilter(AbstractComposableRepositoryPostProcessor.class)
		);
		postProcessors.addExcludeFilter(
				new AbstractClassTestingTypeFilter() {
					@Override
					protected boolean match(ClassMetadata metadata) {
						return metadata.getClassName()
						               .equals(AbstractComposableRepositoryPostProcessor.class.getName());
					}
				}
		);
		postProcessors.setResourceLoader(resourceLoader);

		List<String> packagesToScan = new ArrayList<>();
		packagesToScan.add("reactor.data.spring");

		String[] basePackages = (String[])attrs.get("basePackages");
		if(basePackages.length == 0) {
			try {
				String s = Class.forName(meta.getClassName()).getPackage().getName();
				basePackages = new String[]{s};
			} catch(ClassNotFoundException e) {
			}
		}
		Collections.addAll(packagesToScan, basePackages);

		if(!registry.containsBeanDefinition(REACTOR_ENV)) {
			BeanDefinitionBuilder envBeanDef = BeanDefinitionBuilder.rootBeanDefinition(Environment.class);
			registry.registerBeanDefinition(REACTOR_ENV, envBeanDef.getBeanDefinition());
		}
		if(!registry.containsBeanDefinition(EXECUTOR_BEAN)) {
			int threads = Environment.PROCESSORS * 4;
			BeanDefinitionBuilder executorBeanDef = BeanDefinitionBuilder.rootBeanDefinition(ThreadPoolExecutor.class);
			executorBeanDef.addConstructorArgValue(threads);
			executorBeanDef.addConstructorArgValue(threads);
			executorBeanDef.addConstructorArgValue(0L);
			executorBeanDef.addConstructorArgValue(TimeUnit.MILLISECONDS);
			executorBeanDef.addConstructorArgValue(BlockingQueueFactory.<Runnable>createQueue());
			executorBeanDef.addConstructorArgValue(new NamedDaemonThreadFactory("reactor-data-io"));
			registry.registerBeanDefinition(EXECUTOR_BEAN, executorBeanDef.getBeanDefinition());
		}

		String dispatcher = attrs.get("dispatcher").toString();

		for(String basePackage : packagesToScan) {
			for(BeanDefinition beanDef : postProcessors.findCandidateComponents(basePackage)) {
				BeanDefinitionBuilder factoryBeanDef = BeanDefinitionBuilder.rootBeanDefinition(
						beanDef.getBeanClassName()
				);

				factoryBeanDef.addConstructorArgReference(REACTOR_ENV);
				factoryBeanDef.addConstructorArgValue(dispatcher);
				factoryBeanDef.addConstructorArgReference(EXECUTOR_BEAN);
				factoryBeanDef.addConstructorArgValue(basePackages);

				registry.registerBeanDefinition(beanDef.getBeanClassName(), factoryBeanDef.getBeanDefinition());
			}
		}
	}

}
