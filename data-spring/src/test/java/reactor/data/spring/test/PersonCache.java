package reactor.data.spring.test;

import reactor.data.core.ComposableCacheRepository;

/**
 * @author Jon Brisbin
 */
public interface PersonCache extends ComposableCacheRepository<Person, String> {
}
