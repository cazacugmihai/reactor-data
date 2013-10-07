package reactor.data.redis.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.Environment;
import reactor.core.composable.Promise;
import reactor.data.spring.config.EnableComposableRepositories;
import reactor.data.spring.redis.RedisClientFactoryBean;
import reactor.function.Consumer;
import reactor.function.support.Boundary;
import reactor.function.support.Tap;
import reactor.spring.context.config.EnableReactor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RedisComposableRepositoryTests.TestConfig.class})
public class RedisComposableRepositoryTests {

	private static final Logger LOG = LoggerFactory.getLogger(RedisComposableRepositoryTests.class);

	@Autowired
	GlobalCounters counters;
	@Autowired
	PersonCache    personCache;
	@Autowired
	PersonBroker   personBroker;
	Boundary b;

	@Before
	public void start() {
		b = new Boundary();
	}

	@Test
	public void manipulatesCounters() throws InterruptedException {
		int times = 10;
		for(int i = 0; i < times; i++) {
			counters.incr("test");
		}

		// Make sure everything got updated
		Thread.sleep(500);

		Promise<Long> p = counters.get("test")
		                          .onSuccess(b.bind(new Consumer<Long>() {
			                          @Override
			                          public void accept(Long l) {
				                          LOG.info("Found counter test={}", l);
			                          }
		                          }));

		assertThat("Counter value was retrieved", b.await(5, TimeUnit.SECONDS));
		assertThat("Counter was incremented", p.get(), greaterThan(9l));
	}

	@Test
	public void exposesObjectCache() throws InterruptedException {
		Person p = new Person("1", "John Doe");
		personCache.set(p.getId(), p)
		           .onSuccess(b.bind(new Consumer<Person>() {
			           @Override
			           public void accept(Person p) {
				           LOG.info("Set person in cache {}", p);
			           }
		           }));
		Promise<Person> p2 = personCache.get(p.getId())
		                                .consume(b.bind(new Consumer<Person>() {
			                                @Override
			                                public void accept(Person p) {
				                                LOG.info("Found person in cache {}", p);
			                                }
		                                }));

		assertThat("Cache was updated and result retrieved", b.await(5, TimeUnit.SECONDS));
		assertThat("Cached Person is the same as the in-scope Person", p, equalTo(p2.get()));
	}

	@Test
	public void exposesKeysItHasSeen() throws InterruptedException {
		Person p = new Person("1", "John Doe");
		personCache.set(p.getId(), p).await(5, TimeUnit.SECONDS);
		List<String> keys = personCache.keys().await(5, TimeUnit.SECONDS);

		assertThat("Keys contains some values", keys, not(empty()));
	}

	@Test
	public void exposesMessaging() throws InterruptedException {
		Person p = new Person("1", "John Doe");

		Tap<Person> tap = personBroker.receive("*")
		                              .consume(b.bind(new Consumer<Person>() {
			                              @Override
			                              public void accept(Person person) {
				                              LOG.info("Found Person {}", person);
			                              }
		                              }))
		                              .tap();
		personBroker.send("person", p);

		assertThat("Consumer was invoked", b.await(5, TimeUnit.SECONDS));
		assertThat("Person was retrieved", tap.get(), notNullValue());
		assertThat("Person is correct", tap.get(), equalTo(p));
	}

	@Configuration
	@EnableReactor
	@EnableComposableRepositories(basePackages = {"reactor.data.redis.test"}, dispatcher = Environment.RING_BUFFER)
	static class TestConfig {

		@Bean
		public RedisClientFactoryBean redisClient() {
			return new RedisClientFactoryBean("localhost");
		}

	}

}
