package reactor.data.spring.test;

import reactor.core.Composable;
import reactor.data.spring.StreamCrudRepository;

/**
 * @author Jon Brisbin
 */
public interface ComposablePersonRepository extends StreamCrudRepository<Person, Long> {

	Composable<Person> findByName(String name);

}
