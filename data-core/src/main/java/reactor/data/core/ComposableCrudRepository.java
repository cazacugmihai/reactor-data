package reactor.data.core;

import reactor.core.composable.Composable;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;

import java.io.Serializable;

/**
 * A {@code ComposableCrudRepository} is a way to manage mappable entities.
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public interface ComposableCrudRepository<V, K extends Serializable> extends ComposableRepository<V, K> {

	/**
	 * Saves all given entities.
	 *
	 * @param entities
	 * 		a {@link reactor.core.composable.Promise} or {@link reactor.core.composable.Stream} of entities to save.
	 *
	 * @return the saved entities
	 *
	 * @throws IllegalArgumentException
	 * 		in case the given entity is (@literal null}.
	 */
	<S extends V> Stream<S> save(Composable<S> entities);

	/**
	 * Retrieves an entity by its key.
	 *
	 * @param key
	 * 		must not be {@literal null}.
	 *
	 * @return the entity with the given key or {@literal null} if none found
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code key} is {@literal null}
	 */
	Promise<V> findOne(K key);

	/**
	 * Returns whether an entity with the given key exists.
	 *
	 * @param key
	 * 		must not be {@literal null}.
	 *
	 * @return true if an entity with the given key exists, {@literal false} otherwise
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code key} is {@literal null}
	 */
	Promise<Boolean> exists(K key);

	/**
	 * Returns all instances of the type.
	 *
	 * @return all entities
	 */
	Stream<V> findAll();

	/**
	 * Returns all instances of the type with the given keys.
	 *
	 * @param keys
	 *
	 * @return
	 */
	Stream<V> findAll(Iterable<K> keys);

	/**
	 * Returns the number of entities available.
	 *
	 * @return the number of entities
	 */
	Promise<Long> count();

	/**
	 * Deletes the entity with the given key.
	 *
	 * @param key
	 * 		must not be {@literal null}.
	 *
	 * @return a {@link reactor.core.composable.Promise} fulfilled by the object deleted if it existed
	 *
	 * @throws IllegalArgumentException
	 * 		in case the given {@code key} is {@literal null}
	 */
	Promise<V> delete(K key);

	/**
	 * Deletes the given entities.
	 *
	 * @param entities
	 *
	 * @throws IllegalArgumentException
	 * 		in case the given {@link Iterable} is (@literal null}.
	 */
	Promise<Void> delete(Composable<? extends V> entities);

	/**
	 * Deletes all entities managed by the repository.
	 */
	Promise<Void> deleteAll();

}
