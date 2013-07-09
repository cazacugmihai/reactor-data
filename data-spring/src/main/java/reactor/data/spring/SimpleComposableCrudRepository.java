package reactor.data.spring;

import org.springframework.data.repository.CrudRepository;
import reactor.Fn;
import reactor.R;
import reactor.core.Reactor;
import reactor.core.Environment;
import reactor.core.composable.*;
import reactor.function.*;

import java.io.Serializable;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
class SimpleComposableCrudRepository<T, ID extends Serializable> implements ComposableCrudRepository<T, ID> {

	private final Environment           env;
	private final Reactor               reactor;
	private final CrudRepository<T, ID> delegateRepository;

	SimpleComposableCrudRepository(Environment env, String dispatcher, CrudRepository<T, ID> delegateRepository) {
		this.env = env;
		this.delegateRepository = delegateRepository;

		this.reactor = R.reactor()
										.env(env)
										.dispatcher(dispatcher)
										.get();
	}

	@Override
	public <S extends T> Stream<S> save(Composable<S> entities) {
		final Deferred<S,Stream<S>> s = Streams.<S>defer()
															 .env(env)
															 .reactor(reactor)
															 .get();

		entities.consume(new Consumer<S>() {
			@Override
			public void accept(S entity) {
				s.accept(delegateRepository.save(entity));
			}
		});

		return s.compose();
	}

	@Override
	public Promise<T> findOne(ID id) {
		return Promises.success(id)
									 .env(env)
									 .reactor(reactor)
									 .get()
									 .map(new Function<ID, T>() {
										 @Override
										 public T apply(ID id) {
											 return delegateRepository.findOne(id);
										 }
									 });
	}

	@Override
	public Promise<Boolean> exists(ID id) {
		return Promises.success(id)
									 .env(env)
									 .reactor(reactor)
									 .get()
									 .map(new Function<ID, Boolean>() {
										 @Override
										 public Boolean apply(ID id) {
											 return delegateRepository.exists(id);
										 }
									 });
	}

	@Override
	public Stream<T> findAll() {
		final Deferred<T,Stream<T>> s = Streams.<T>defer()
															 .env(env)
															 .reactor(reactor)
															 .get();

		Consumer<Void> consumer = new Consumer<Void>() {
			@Override
			public void accept(Void v) {
				for (T t : delegateRepository.findAll()) {
					s.accept(t);
				}
			}
		};
		Fn.schedule(consumer, null, reactor);

		return s.compose();
	}

	@Override
	public Stream<T> findAll(final Iterable<ID> ids) {
		final Deferred<T,Stream<T>> s = Streams.<T>defer()
															 .env(env)
															 .reactor(reactor)
															 .get();

		Consumer<Void> consumer = new Consumer<Void>() {
			@Override
			public void accept(Void v) {
				for (T t : delegateRepository.findAll(ids)) {
					s.accept(t);
				}
			}
		};
		Fn.schedule(consumer, null, reactor);

		return s.compose();
	}

	@Override
	public Promise<Long> count() {
		final Deferred<Long, Promise<Long>> p = Promises.<Long>defer()
																		.env(env)
																		.reactor(reactor)
																		.get();

		Consumer<Void> consumer = new Consumer<Void>() {
			@Override
			public void accept(Void v) {
				p.accept(delegateRepository.count());
			}
		};
		Fn.schedule(consumer, null, reactor);

		return p.compose();
	}

	@Override
	public Promise<Void> delete(ID id) {
		return Promises.success(id)
									 .env(env)
									 .reactor(reactor)
									 .get()
									 .map(new Function<ID, Void>() {
										 @Override
										 public Void apply(ID id) {
											 delegateRepository.delete(id);
											 return null;
										 }
									 });
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Promise<Void> delete(Composable<? extends T> entities) {
		final Deferred<Void, Promise<Void>> p = Promises.<Void>defer()
																		.env(env)
																		.reactor(reactor)
																		.get();

		entities.consume(new Consumer() {
			@Override
			public void accept(Object o) {
				delegateRepository.delete((T) o);
				p.accept((Void) null);
			}
		});
		return p.compose();
	}

	@Override
	public Promise<Void> deleteAll() {
		final Deferred<Void, Promise<Void>> c = Promises.<Void>defer()
																		.env(env)
																		.reactor(reactor)
																		.get();

		Consumer<Void> consumer = new Consumer<Void>() {
			@Override
			public void accept(Void aVoid) {
				delegateRepository.deleteAll();
				c.accept((Void) null);
			}
		};
		Fn.schedule(consumer, null, reactor);

		return c.compose();
	}

}
