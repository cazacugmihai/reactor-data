package reactor.data.spring.test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.util.AssertionErrors.*;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

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
import reactor.core.composable.Promise;
import reactor.core.composable.spec.Promises;
import reactor.data.spring.config.EnableComposableRepositories;
import reactor.function.support.Boundary;
import reactor.function.support.Tap;
import reactor.spring.context.config.EnableReactor;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ComposableRepositoryTests.TestConfig.class})
public class ComposableRepositoryTests {

	private static final Logger LOG = LoggerFactory.getLogger(ComposableRepositoryTests.class);

	@Autowired
	ComposablePersonRepository people;
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
		                      .consume(b.bind(p -> LOG.info("Saved person {}", p)))
		                      .tap();
		assertTrue("Person was saved within the timeout", b.await(1, TimeUnit.SECONDS));
		assertThat("Person was actually saved to the database", t.get().getId(), notNullValue());
	}

	@Test
	public void deletesEntities() throws InterruptedException {
		Tap<Person> t = people.save(personPromise)
		                      .consume(b.bind(p -> LOG.info("Saved person {}", p)))
		                      .tap();
		assertTrue("Person was saved within the timeout", b.await(1, TimeUnit.SECONDS));
		assertThat("Person was actually saved to the database", t.get().getId(), notNullValue());

		Promise<Person> p2 = people.delete(t.get().getId())
		                           .onSuccess(b.<Person>bind(p3 -> LOG.info("Deleted person {}", p3)));
		assertThat("Person was deleted within the timeout", p2.await(1, TimeUnit.SECONDS), notNullValue());
	}

	@Test
	public void queriesEntities() {
		Tap<Person> t = people.findByName("John Doe")
		                      .consume(b.bind(p -> LOG.info("Found person {}", p)))
		                      .tap();
		assertTrue("Person was queried within the timeout", b.await(1, TimeUnit.SECONDS));
		assertThat("Person was actually actually queried", t.get().getId(), notNullValue());
		assertThat("Person was actually actually queried", t.get().getName(), is("John Doe"));
	}


	@Configuration
	@EnableReactor
	@EnableMongoRepositories(basePackages = {"reactor.data.spring.test"})
	@EnableComposableRepositories(basePackages = {"reactor.data.spring.test"})
	static class TestConfig {

		@Bean MongoTemplate mongoTemplate() throws UnknownHostException {
			return new MongoTemplate(new Mongo(), "reactor");
		}

	}

}
