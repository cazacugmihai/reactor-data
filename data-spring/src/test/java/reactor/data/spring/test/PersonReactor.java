package reactor.data.spring.test;

import reactor.data.core.ComposableEventRepository;

/**
 * @author Jon Brisbin
 */
public interface PersonReactor extends ComposableEventRepository<Person, String> {
}
