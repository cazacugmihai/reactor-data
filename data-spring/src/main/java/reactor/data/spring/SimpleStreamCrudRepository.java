package reactor.data.spring;

import org.springframework.data.repository.CrudRepository;
import reactor.Fn;
import reactor.core.Stream;
import reactor.R;
import reactor.core.Reactor;
import reactor.core.Streams;
import reactor.fn.Consumer;
import reactor.fn.Function;

import java.io.Serializable;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
class SimpleStreamCrudRepository<T, ID extends Serializable> implements StreamCrudRepository<T, ID> {

	private final Reactor reactor = R.reactor().get();
	private final CrudRepository<T, ID> delegateRepository;

	SimpleStreamCrudRepository(CrudRepository<T, ID> delegateRepository) {
		this.delegateRepository = delegateRepository;
	}

	@Override

	public <S extends T> Stream<S> save(Stream<S> entities) {
		return entities.map(new Function<S,S>(){
			@Override
			public S apply(S entity) {
				return delegateRepository.save(entity);
			}
		});
	}

	@Override
	public Stream<T> findOne(ID id) {
		return Streams.defer(id)
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
	public Stream<Boolean> exists(ID id) {
		return Streams.defer(id)
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
		final Stream<T> c = Streams.<T>defer().using(reactor).get();
		Consumer<Void> consumer = new Consumer<Void>() {
			@Override
			public void accept(Void v) {
				for (T t : delegateRepository.findAll()) {
					c.accept(t);
				}
			}
		};
		Fn.schedule(consumer, null, reactor);
		return c;
	}

	@Override
	public Stream<T> findAll(Stream<ID> ids) {
		final Stream<T> c = Streams.<T>defer().using(reactor).get();
		ids.consume(new Consumer<ID>() {
			@Override
			public void accept(ID id) {
				c.accept(delegateRepository.findOne(id));
			}
		});
		return c;
	}

	@Override
	public Stream<Long> count() {
		final Stream<Long> c = Streams.<Long>defer().using(reactor).get();
		Consumer<Void> consumer = new Consumer<Void>() {
			@Override
			public void accept(Void v) {
				c.accept(delegateRepository.count());
			}
		};
		Fn.schedule(consumer, null, reactor);
		return c;
	}

	@Override
	public Stream<Void> delete(ID id) {
		return Streams.defer(id)
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
	public Stream<Void> delete(Stream<? extends T> entities) {
		final Stream<Void> c = Streams.<Void>defer().using(reactor).get();
		entities.consume(new Consumer() {
			@Override
			public void accept(Object o) {
				delegateRepository.delete((T) o);
				c.accept((Void) null);
			}
		});
		return c;
	}

	@Override
	public Stream<Void> deleteAll() {
		final Stream<Void> c = Streams.<Void>defer().using(reactor).get();
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
