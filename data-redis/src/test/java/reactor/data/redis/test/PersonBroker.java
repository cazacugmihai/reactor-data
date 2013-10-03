package reactor.data.redis.test;

import reactor.data.core.ComposableMessagingRepository;

/**
 * @author Jon Brisbin
 */
public interface PersonBroker extends ComposableMessagingRepository<Person, String> {
}
