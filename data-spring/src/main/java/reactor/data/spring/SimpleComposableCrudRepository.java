package reactor.data.spring;

import org.springframework.data.repository.CrudRepository;
import reactor.Fn;
import reactor.R;
import reactor.core.*;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.fn.dispatch.Dispatcher;

import java.io.Serializable;

/**
 * @author Jon Brisbin
 */
class SimpleComposableCrudRepository<T, ID extends Serializable> implements ComposableCrudRepository<T, ID> {

	private final Environment           env;
	private final String                dispatcher;
	private final Reactor               reactor;
	private final CrudRepository<T, ID> delegateRepository;

	SimpleComposableCrudRepository(Environment env, String dispatcher, CrudRepository<T, ID> delegateRepository) {
		this.env = env;
		this.dispatcher = dispatcher;
		this.delegateRepository = delegateRepository;

		this.reactor = R.reactor()
										.using(env)
										.dispatcher(dispatcher)
										.get();
	}

	@Override
	public <S extends T> Stream<S> save(Composable<S> entities) {
		final Stream<S> s = Streams.<S>defer()
															 .using(env)
															 .using(reactor)
															 .get();

		entities.consume(new Consumer<S>() {
			@Override
			public void accept(S entity) {
				s.accept(delegateRepository.save(entity));
			}
		});

		return s;
	}

	@Override
	public Promise<T> findOne(ID id) {
		return Promises.success(id)
									 .using(env)
									 .using(reactor)
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
									 .using(env)
									 .using(reactor)
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
		final Stream<T> s = Streams.<T>defer()
															 .using(env)
															 .using(reactor)
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

		return s;
	}

	@Override
	public Stream<T> findAll(final Iterable<ID> ids) {
		final Stream<T> s = Streams.<T>defer()
															 .using(env)
															 .using(reactor)
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

		return s;
	}

	@Override
	public Promise<Long> count() {
		final Promise<Long> p = Promises.<Long>defer()
																		.using(env)
																		.using(reactor)
																		.get();

		Consumer<Void> consumer = new Consumer<Void>() {
			@Override
			public void accept(Void v) {
				p.accept(delegateRepository.count());
			}
		};
		Fn.schedule(consumer, null, reactor);

		return p;
	}

	@Override
	public Promise<Void> delete(ID id) {
		return Promises.success(id)
									 .using(env)
									 .using(reactor)
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
		final Promise<Void> p = Promises.<Void>defer()
																		.using(env)
																		.using(reactor)
																		.get();

		entities.consume(new Consumer() {
			@Override
			public void accept(Object o) {
				delegateRepository.delete((T) o);
				p.accept((Void) null);
			}
		});
		return p;
	}

	@Override
	public Promise<Void> deleteAll() {
		final Promise<Void> c = Promises.<Void>defer()
																		.using(env)
																		.using(reactor)
																		.get();

		Consumer<Void> consumer = new Consumer<Void>() {
			@Override
			public void accept(Void aVoid) {
				delegateRepository.deleteAll();
				c.accept((Void) null);
			}
		};
		Fn.schedule(consumer, null, reactor);

		return c;
	}

}
