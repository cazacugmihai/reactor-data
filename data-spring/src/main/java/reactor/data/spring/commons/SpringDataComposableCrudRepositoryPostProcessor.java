package reactor.data.spring.commons;

import org.aopalliance.aop.Advice;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.repository.support.Repositories;
import reactor.core.Environment;
import reactor.data.core.ComposableCrudRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 */
public class SpringDataComposableCrudRepositoryPostProcessor<T>
		extends AbstractComposableRepositoryPostProcessor<T, ComposableCrudRepository<T, ? extends Serializable>>
		implements InitializingBean {

	private Repositories repositories;

	public SpringDataComposableCrudRepositoryPostProcessor(Environment env,
	                                                       String dispatcher,
	                                                       Executor executor,
	                                                       String[] basePackages) {
		super(env, dispatcher, executor, basePackages);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.repositories = new Repositories(beanFactory);
	}

	@Override
	protected Class<?> getRepositoryType() {
		return ComposableCrudRepository.class;
	}

	@Override
	protected String getProviderName() {
		return "spring";
	}

	@Override
	protected ComposableCrudRepository<T, ? extends Serializable> createRepositoryProxy(Class<T> managedType) {
		return new SpringDataComposableCrudRepository<>(getEnvironment(),
		                                                getDispatcher(),
		                                                getExecutor(),
		                                                repositories,
		                                                managedType);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Advice> getAdvice(Class<ComposableCrudRepository<T, ? extends Serializable>> repoType,
	                                 ComposableCrudRepository<T, ? extends Serializable> composableRepo) {
		return Arrays.<Advice>asList(new SpringDataRepositoryQueryMethodMethodInterceptor<>(
				getEnvironment(),
				getDispatcher(),
				getExecutor(),
				repoType,
				(SpringDataComposableCrudRepository<Object, Serializable>)composableRepo
		));
	}

}
