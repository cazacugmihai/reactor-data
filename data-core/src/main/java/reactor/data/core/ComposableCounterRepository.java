package reactor.data.core;

import reactor.core.composable.Promise;
import reactor.function.Consumer;

/**
 * @author Jon Brisbin
 */
public interface ComposableCounterRepository extends ComposableRepository<Long, String> {

	ComposableCounterRepository decr(String name);

	ComposableCounterRepository incr(String name);

	Consumer<Long> add(String name);

	Promise<Long> get(String name);

	Promise<Long> remove(String name);

}
