package reactor.data.spring.test;

import com.mongodb.Mongo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.Environment;
import reactor.core.composable.Promise;
import reactor.core.composable.spec.Promises;
import reactor.data.spring.config.EnableComposableRepositories;
import reactor.data.spring.redis.RedisClientFactoryBean;
import reactor.function.Consumer;
import reactor.function.support.Boundary;
import reactor.function.support.Tap;
import reactor.spring.context.config.EnableReactor;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ComposableRepositoryTests.TestConfig.class})
public class ComposableRepositoryTests {

	private static final Logger LOG = LoggerFactory.getLogger(ComposableRepositoryTests.class);

	@Autowired
	PersonRepository           personRepo;
	@Autowired
	ComposablePersonRepository people;
	@Autowired
	GlobalCounters             counters;
	@Autowired
	PersonCache                personCache;
	Boundary        b;
	Promise<Person> personPromise;

	@Before
	public void start() {
		b = new Boundary();
		personPromise = Promises.success(new Person("John Doe")).get();
	}

	@Test
	public void savesEntities() {
		Tap<Person> t = people.save(personPromise)
		                      .consume(b.bind(new Consumer<Person>() {
			                      @Override
			                      public void accept(Person p) {
				                      LOG.info("Saved person {}", p);
			                      }
		                      }))
		                      .tap();
		assertTrue("Person was saved within the timeout", b.await(15, TimeUnit.SECONDS));
		assertThat("Person was actually saved to the database", t.get().getId(), notNullValue());
	}

	@Test
	public void deletesEntities() throws InterruptedException {
		Tap<Person> t = people.save(personPromise)
		                      .consume(b.bind(new Consumer<Person>() {
			                      @Override
			                      public void accept(Person p) {
				                      LOG.info("Saved person {}", p);
			                      }
		                      }))
		                      .tap();
		assertTrue("Person was saved within the timeout", b.await(5, TimeUnit.SECONDS));
		assertThat("Person was actually saved to the database", t.get().getId(), notNullValue());

		Promise<Person> p2 = people.delete(t.get().getId())
		                           .onSuccess(b.<Person>bind(new Consumer<Person>() {
			                           @Override
			                           public void accept(Person p) {
				                           LOG.info("Deleted person {}", p);
			                           }
		                           }));
		assertThat("Person was deleted within the timeout", p2.await(5, TimeUnit.SECONDS), notNullValue());
	}

	@Test
	public void queriesEntities() {
		people.save(personPromise)
		      .consume(b.bind(new Consumer<Person>() {
			      @Override
			      public void accept(Person p) {
				      LOG.info("Saved person {}", p);
			      }
		      }));

		Tap<Person> t = people.findByName("John Doe")
		                      .consume(b.bind(new Consumer<Person>() {
			                      @Override
			                      public void accept(Person p) {
				                      LOG.info("Found person {}", p);
			                      }
		                      }))
		                      .tap();
		assertTrue("Person was queried within the timeout", b.await(5, TimeUnit.SECONDS));
		assertThat("Person was actually actually queried", t.get().getId(), notNullValue());
		assertThat("Person was actually actually queried", t.get().getName(), is("John Doe"));
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
		Person p = personRepo.save(new Person("John Doe"));
		personCache.set(p.getId(), p)
		           .onSuccess(b.bind(new Consumer<Person>() {
			           @Override
			           public void accept(Person p) {
				           LOG.info("Set person in cache {}", p);
			           }
		           }));
		Promise<Person> p2 = personCache.get(p.getId())
		                                .onSuccess(b.bind(new Consumer<Person>() {
			                                @Override
			                                public void accept(Person p) {
				                                LOG.info("Found person in cache {}", p);
			                                }
		                                }));

		assertThat("Cache was updated and result retrieved", b.await(15, TimeUnit.SECONDS));
		assertThat("Cached Person is the same as the in-scope Person", p, equalTo(p2.get()));
	}

	@Test
	public void exposesKeysItHasSeen() throws InterruptedException {
		Person p = personRepo.save(new Person("John Doe"));
		personCache.set(p.getId(), p).await(5, TimeUnit.SECONDS);
		List<String> keys = personCache.keys().await(5, TimeUnit.SECONDS);

		assertThat("Keys contains some values", keys, not(empty()));
	}

	@Configuration
	@EnableReactor
	@EnableMongoRepositories(basePackages = {"reactor.data.spring.test"})
	@EnableComposableRepositories(basePackages = {"reactor.data.spring.test"}, dispatcher = Environment.RING_BUFFER)
	static class TestConfig {

		@Bean
		MongoTemplate mongoTemplate() throws UnknownHostException {
			return new MongoTemplate(new Mongo(), "reactor");
		}

		@Bean
		RedisClientFactoryBean redisClient() {
			return new RedisClientFactoryBean("localhost");
		}

	}

}
