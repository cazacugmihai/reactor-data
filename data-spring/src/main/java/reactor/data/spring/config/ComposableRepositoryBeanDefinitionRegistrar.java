package reactor.data.spring.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import reactor.core.Environment;
import reactor.data.spring.ComposableRepository;
import reactor.data.spring.ComposableRepositoryFactoryBean;
import reactor.spring.beans.factory.HashWheelTimerFactoryBean;

/**
 * @author Jon Brisbin
 */
public class ComposableRepositoryBeanDefinitionRegistrar
		implements ImportBeanDefinitionRegistrar,
		           ResourceLoaderAware {

	private static final String REACTOR_ENV = "reactorEnv";
	private static final String TIMER_BEAN  = "reactorHashWheelTimer";

	private final ClassLoader classLoader = getClass().getClassLoader();
	private ResourceLoader resourceLoader;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {

		Map<String, Object> attrs = meta.getAnnotationAttributes(EnableComposableRepositories.class.getName());

		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				return beanDefinition.getMetadata().isIndependent();
			}
		};
		provider.addIncludeFilter(new AssignableTypeFilter(ComposableRepository.class));
		provider.setResourceLoader(resourceLoader);

		String[] basePackages = (String[])attrs.get("basePackages");
		if(basePackages.length == 0) {
			String s = "";
			try {
				s = Class.forName(meta.getClassName()).getPackage().getName();
			} catch(ClassNotFoundException e) {
			}
			basePackages = new String[]{s};
		}

		if(!registry.containsBeanDefinition(REACTOR_ENV)) {
			BeanDefinitionBuilder envBeanDef = BeanDefinitionBuilder.rootBeanDefinition(Environment.class);
			registry.registerBeanDefinition(REACTOR_ENV, envBeanDef.getBeanDefinition());
		}
		if(!registry.containsBeanDefinition(TIMER_BEAN)) {
			BeanDefinitionBuilder envBeanDef = BeanDefinitionBuilder.rootBeanDefinition(HashWheelTimerFactoryBean.class);
			envBeanDef.addConstructorArgValue(Environment.PROCESSORS);
			envBeanDef.addConstructorArgValue(50);
			registry.registerBeanDefinition(TIMER_BEAN, envBeanDef.getBeanDefinition());
		}

		String dispatcher = attrs.get("dispatcher").toString();

		for(String basePackage : basePackages) {
			for(BeanDefinition beanDef : provider.findCandidateComponents(basePackage)) {
				BeanDefinitionBuilder factoryBeanDef = BeanDefinitionBuilder.rootBeanDefinition(
						ComposableRepositoryFactoryBean.class.getName()
				);
				factoryBeanDef.addConstructorArgReference(REACTOR_ENV);
				factoryBeanDef.addConstructorArgValue(dispatcher);
				factoryBeanDef.addConstructorArgReference(TIMER_BEAN);
				factoryBeanDef.addConstructorArgValue(ClassUtils.resolveClassName(beanDef.getBeanClassName(), classLoader));

				registry.registerBeanDefinition(beanDef.getBeanClassName(), factoryBeanDef.getBeanDefinition());
			}
		}
	}

}
