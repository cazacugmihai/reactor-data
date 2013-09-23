package reactor.data.spring;

import org.springframework.data.repository.CrudRepository;
import reactor.core.Environment;
import reactor.core.HashWheelTimer;
import reactor.core.Reactor;
import reactor.core.composable.Composable;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.Promises;
import reactor.core.composable.spec.Streams;
import reactor.core.spec.Reactors;
import reactor.function.Consumer;

import java.io.Serializable;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
class SimpleComposableCrudRepository<T, ID extends Serializable> implements ComposableCrudRepository<T, ID> {

	private final Environment           env;
	private final HashWheelTimer        timer;
	private final CrudRepository<T, ID> delegateRepository;
	private final Reactor               reactor;

	SimpleComposableCrudRepository(Environment env,
	                               String dispatcher,
	                               HashWheelTimer timer,
	                               CrudRepository<T, ID> delegateRepository) {
		this.env = env;
		this.timer = timer;
		this.delegateRepository = delegateRepository;

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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				if (entities instanceof Promise) {
					((Promise<S>) entities).onSuccess(new Consumer<S>() {
						@Override
						public void accept(S s) {
							d.accept(delegateRepository.save(s));
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
							d.accept(delegateRepository.save(s));
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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				d.accept(delegateRepository.findOne(id));
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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				d.accept(delegateRepository.exists(id));
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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				for (T t : delegateRepository.findAll()) {
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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				for (T t : delegateRepository.findAll(ids)) {
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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				d.accept(delegateRepository.count());
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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				T t = delegateRepository.findOne(id);
				delegateRepository.delete(id);
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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				if (entities instanceof Promise) {
					((Promise<T>) entities).onSuccess(new Consumer<T>() {
						@Override
						public void accept(T t) {
							delegateRepository.delete(t);
							d.accept((Void) null);
						}
					}).onError(new Consumer<Throwable>() {
						@Override
						public void accept(Throwable t) {
							d.accept(t);
						}
					});
				} else {
					((Stream<T>) entities).consume(new Consumer<T>() {
						@Override
						public void accept(T t) {
							delegateRepository.delete(t);
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

		timer.submit(new Consumer<Long>() {
			@Override
			public void accept(Long l) {
				delegateRepository.deleteAll();
				d.accept((Void) null);
			}
		});

		return d.compose();
	}

}
