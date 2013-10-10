package reactor.data.spring.reactor;

import reactor.core.Environment;
import reactor.data.core.ComposableEventRepository;
import reactor.data.core.ComposableRepository;
import reactor.data.reactor.ReactorComposableEventRepository;
import reactor.data.spring.AbstractComposableRepositoryPostProcessor;

import java.io.Serializable;
import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 */
public class ReactorComposableEventRepositoryPostProcessor<V, K extends Serializable>
		extends AbstractComposableRepositoryPostProcessor<V, ComposableRepository<V, K>> {

	public ReactorComposableEventRepositoryPostProcessor(Environment env,
	                                                     String dispatcher,
	                                                     Executor executor,
	                                                     String[] basePackages) {
		super(env, dispatcher, executor, basePackages);
	}

	@Override
	protected Class<?> getRepositoryType() {
		return ComposableEventRepository.class;
	}

	@Override
	protected String getProviderName() {
		return "reactor";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ComposableRepository<V, K> createRepositoryProxy(Class<V> managedType) {
		return (ComposableRepository<V, K>)new ReactorComposableEventRepository<>(getEnvironment(), getDispatcher());
	}

}
