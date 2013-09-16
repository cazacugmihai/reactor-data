package reactor.data.spring.test;

import reactor.core.composable.Stream;
import reactor.data.spring.ComposableCrudRepository;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public interface ComposablePersonRepository extends ComposableCrudRepository<Person, String> {

	Stream<Person> findByName(String name);

}
