package reactor.data.redis.test;

import reactor.data.core.ComposableCacheRepository;

/**
 * @author Jon Brisbin
 */
public interface PersonCache extends ComposableCacheRepository<Person, String> {
}
