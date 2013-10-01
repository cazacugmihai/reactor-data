package reactor.data.spring.commons;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.Repositories;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.composable.Composable;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.Promises;
import reactor.core.composable.spec.Streams;
import reactor.core.spec.Reactors;
import reactor.data.core.ComposableCrudRepository;
import reactor.function.Consumer;

import java.io.Serializable;
import java.util.concurrent.Executor;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
class SpringDataComposableCrudRepository<T, ID extends Serializable> implements ComposableCrudRepository<T, ID> {

	private final Environment           env;
	private final Executor              executor;
	private final Reactor               reactor;
	private final Repositories          repositories;
	private final Class<?>              managedType;
	private       CrudRepository<T, ID> delegateRepository;

	SpringDataComposableCrudRepository(
			Environment env,
			String dispatcher,
			Executor executor,
			Repositories repositories,
			Class<?> managedType
	) {
		this.env = env;
		this.executor = executor;
		this.repositories = repositories;
		this.managedType = managedType;

		this.reactor = Reactors.reactor()
		                       .env(env)
		                       .dispatcher(dispatcher)
		                       .get();
	}

	@Override
	public <S extends T> Stream<S> save(final Composable<S> entities) {
		final Deferred<S, Stream<S>> d = Streams.<S>defer()
		                                        .env(env)
		                                        .dispatcher(reactor.getDispatcher())
		                                        .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				if(entities instanceof Promise) {
					((Promise<S>)entities).onSuccess(new Consumer<S>() {
						@Override
						public void accept(S s) {
							d.accept(getDelegateRepository().save(s));
						}
					}).onError(new Consumer<Throwable>() {
						@Override
						public void accept(Throwable t) {
							d.accept(t);
						}
					});
				} else {
					entities.consume(new Consumer<S>() {
						@Override
						public void accept(S s) {
							d.accept(getDelegateRepository().save(s));
						}
					});
				}
			}
		});

		return d.compose();
	}

	@Override
	public Promise<T> findOne(final ID id) {
		final Deferred<T, Promise<T>> d = Promises.<T>defer()
		                                          .env(env)
		                                          .dispatcher(reactor.getDispatcher())
		                                          .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				d.accept(getDelegateRepository().findOne(id));
			}
		});

		return d.compose();
	}

	@Override
	public Promise<Boolean> exists(final ID id) {
		final Deferred<Boolean, Promise<Boolean>> d = Promises.<Boolean>defer()
		                                                      .env(env)
		                                                      .dispatcher(reactor.getDispatcher())
		                                                      .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				d.accept(getDelegateRepository().exists(id));
			}
		});

		return d.compose();
	}

	@Override
	public Stream<T> findAll() {
		final Deferred<T, Stream<T>> d = Streams.<T>defer()
		                                        .env(env)
		                                        .dispatcher(reactor.getDispatcher())
		                                        .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				for(T t : getDelegateRepository().findAll()) {
					d.accept(t);
				}
			}
		});

		return d.compose();
	}

	@Override
	public Stream<T> findAll(final Iterable<ID> ids) {
		final Deferred<T, Stream<T>> d = Streams.<T>defer()
		                                        .env(env)
		                                        .dispatcher(reactor.getDispatcher())
		                                        .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				for(T t : getDelegateRepository().findAll(ids)) {
					d.accept(t);
				}
			}
		});

		return d.compose();
	}

	@Override
	public Promise<Long> count() {
		final Deferred<Long, Promise<Long>> d = Promises.<Long>defer()
		                                                .env(env)
		                                                .dispatcher(reactor.getDispatcher())
		                                                .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				d.accept(getDelegateRepository().count());
			}
		});

		return d.compose();
	}

	@Override
	public Promise<T> delete(final ID id) {
		final Deferred<T, Promise<T>> d = Promises.<T>defer()
		                                          .env(env)
		                                          .dispatcher(reactor.getDispatcher())
		                                          .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				T t = getDelegateRepository().findOne(id);
				getDelegateRepository().delete(id);
				d.accept(t);
			}
		});

		return d.compose();
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Promise<Void> delete(final Composable<? extends T> entities) {
		final Deferred<Void, Promise<Void>> d = Promises.<Void>defer()
		                                                .env(env)
		                                                .dispatcher(reactor.getDispatcher())
		                                                .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				if(entities instanceof Promise) {
					((Promise<T>)entities).onSuccess(new Consumer<T>() {
						@Override
						public void accept(T t) {
							getDelegateRepository().delete(t);
							d.accept((Void)null);
						}
					}).onError(new Consumer<Throwable>() {
						@Override
						public void accept(Throwable t) {
							d.accept(t);
						}
					});
				} else {
					((Stream<T>)entities).consume(new Consumer<T>() {
						@Override
						public void accept(T t) {
							getDelegateRepository().delete(t);
						}
					});
				}
			}
		});

		return d.compose();
	}

	@Override
	public Promise<Void> deleteAll() {
		final Deferred<Void, Promise<Void>> d = Promises.<Void>defer()
		                                                .env(env)
		                                                .dispatcher(reactor.getDispatcher())
		                                                .get();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				getDelegateRepository().deleteAll();
				d.accept((Void)null);
			}
		});

		return d.compose();
	}

	@SuppressWarnings("unchecked")
	CrudRepository<T, ID> getDelegateRepository() {
		synchronized(this) {
			if(null == delegateRepository) {
				delegateRepository = (CrudRepository<T, ID>)repositories.getRepositoryFor(managedType);
			}
			return delegateRepository;
		}
	}

}
