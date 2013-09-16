package reactor.data.spring;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.composable.Composable;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.Promises;
import reactor.core.composable.spec.Streams;
import reactor.core.spec.Reactors;
import reactor.event.Event;
import reactor.function.Consumer;
import reactor.tuple.Tuple;

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

		this.reactor = Reactors.reactor()
		                       .env(env)
		                       .dispatcher(dispatcher)
		                       .get();
	}

	@Override
	public <S extends T> Stream<S> save(Composable<S> entities) {
		final Deferred<S, Stream<S>> s = Streams.<S>defer()
		                                        .env(env)
		                                        .dispatcher(reactor.getDispatcher())
		                                        .get();

		entities.consume(entity -> s.accept(delegateRepository.save(entity)));

		return s.compose();
	}

	@Override
	public Promise<T> findOne(ID id) {
		return Promises.success(id)
		               .env(env)
		               .dispatcher(reactor.getDispatcher())
		               .get()
		               .map(delegateRepository::findOne);
	}

	@Override
	public Promise<Boolean> exists(ID id) {
		return Promises.success(id)
		               .env(env)
		               .dispatcher(reactor.getDispatcher())
		               .get()
		               .map(delegateRepository::exists);
	}

	@Override
	public Stream<T> findAll() {
		final Deferred<T, Stream<T>> s = Streams.<T>defer()
		                                        .env(env)
		                                        .dispatcher(reactor.getDispatcher())
		                                        .get();

		Consumer<Void> consumer = v -> {
			for(T t : delegateRepository.findAll()) {
				s.accept(t);
			}
		};
		reactor.notify(Tuple.of(consumer, Event.NULL_EVENT));

		return s.compose();
	}

	@Override
	public Stream<T> findAll(final Iterable<ID> ids) {
		final Deferred<T, Stream<T>> s = Streams.<T>defer()
		                                        .env(env)
		                                        .dispatcher(reactor.getDispatcher())
		                                        .get();

		Consumer<Void> consumer = v -> {
			for(T t : delegateRepository.findAll(ids)) {
				s.accept(t);
			}
		};
		reactor.notify(Tuple.of(consumer, Event.NULL_EVENT));

		return s.compose();
	}

	@Override
	public Promise<Long> count() {
		final Deferred<Long, Promise<Long>> p = Promises.<Long>defer()
		                                                .env(env)
		                                                .dispatcher(reactor.getDispatcher())
		                                                .get();

		Consumer<Void> consumer = v -> p.accept(delegateRepository.count());
		reactor.notify(Tuple.of(consumer, Event.NULL_EVENT));

		return p.compose();
	}

	@Override
	public Promise<Void> delete(ID id) {
		return Promises.success(id)
		               .env(env)
		               .dispatcher(reactor.getDispatcher())
		               .get()
		               .map(i -> {
			               delegateRepository.delete(i);
			               return null;
		               });
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Promise<Void> delete(Composable<? extends T> entities) {
		final Deferred<Void, Promise<Void>> p = Promises.<Void>defer()
		                                                .env(env)
		                                                .dispatcher(reactor.getDispatcher())
		                                                .get();

		entities.consume(o -> {
			delegateRepository.delete(o);
			p.accept((Void)null);
		});
		return p.compose();
	}

	@Override
	public Promise<Void> deleteAll() {
		final Deferred<Void, Promise<Void>> c = Promises.<Void>defer()
		                                                .env(env)
		                                                .dispatcher(reactor.getDispatcher())
		                                                .get();

		Consumer<Void> consumer = v -> {
			delegateRepository.deleteAll();
			c.accept((Void)null);
		};
		reactor.notify(Tuple.of(consumer, Event.NULL_EVENT));

		return c.compose();
	}

}
