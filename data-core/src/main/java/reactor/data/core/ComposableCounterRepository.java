package reactor.data.core;

import reactor.core.composable.Promise;
import reactor.function.Consumer;

/**
 * A {@code ComposableCounterRepository} is a simple named counter abstraction. It provides a way to simply and
 * efficiently manage counters that are backed by different implementations.
 *
 * @author Jon Brisbin
 */
public interface ComposableCounterRepository extends ComposableRepository<Long, String> {

	/**
	 * Decrement the named counter by 1.
	 *
	 * @param name
	 * 		The name of the counter.
	 *
	 * @return {@code this}
	 */
	ComposableCounterRepository decr(String name);

	/**
	 * Increment the named counter by 1.
	 *
	 * @param name
	 * 		The name of the counter.
	 *
	 * @return {@code this}
	 */
	ComposableCounterRepository incr(String name);

	/**
	 * Get a {@link reactor.function.Consumer} that can be used to efficiently increment or decrement the named counter.
	 * To increment a counter, call {@link reactor.function.Consumer#accept(Object)} with a positive number. To
	 * decrement, call {@link reactor.function.Consumer#accept(Object)} with a negative number.
	 *
	 * @param name
	 * 		The name of the counter.
	 *
	 * @return the {@link Consumer}
	 */
	Consumer<Long> add(String name);

	/**
	 * Get the current value of a counter.
	 *
	 * @param name
	 * 		The name of the counter.
	 *
	 * @return a {@link reactor.core.composable.Promise} that will be fulfilled by the value of the counter. Might be
	 * {@code null} if no counter has been created with that name.
	 */
	Promise<Long> get(String name);

	/**
	 * Remove the named counter.
	 *
	 * @param name
	 * 		The name of the counter.
	 *
	 * @return a {@link reactor.core.composable.Promise} that will be fulfilled when the counter has been removed.
	 */
	Promise<Long> remove(String name);

}
