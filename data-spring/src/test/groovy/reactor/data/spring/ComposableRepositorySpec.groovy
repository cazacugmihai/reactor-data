package reactor.data.spring

import com.mongodb.Mongo
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import reactor.core.Composable
import reactor.core.Environment
import reactor.core.Promises
import reactor.data.spring.config.EnableComposableRepositories
import reactor.data.spring.test.ComposablePersonRepository
import reactor.data.spring.test.Person
import reactor.fn.Consumer
import reactor.fn.Function
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author Jon Brisbin
 */
class ComposableRepositorySpec extends Specification {

	AnnotationConfigApplicationContext appCtx
	Environment env

	def setup() {
		appCtx = new AnnotationConfigApplicationContext(SpecConfig)
		env = appCtx.getBean(Environment)
	}

	def "generates proxies for ComposableRepositories"() {

		when: "a repository is requested"
		def repo = appCtx.getBean(ComposablePersonRepository)

		then: "a repository is provided"
		null != repo

	}

	def "provides repository methods that return Composables"() {

		given: "a ComposableRepository"
		def people = appCtx.getBean(ComposablePersonRepository)

		when: "an entity is saved"
		def start = System.currentTimeMillis()
		def entity = people.save(Promises.success(new Person(id: 1, name: "John Doe")).get())

		then: "entity has saved without timing out"
		System.currentTimeMillis() - start < 5000

		when: "an entity is requested"
		entity = people.findOne(1)
		def name = entity.map({ Person p ->
			p.name
		} as Function<Person, String>)

		then: "entity should have a name property"
		name.await(1, TimeUnit.SECONDS) == "John Doe"

		when: "a finder method is called"
		def latch = new CountDownLatch(1)
		Person person
		people.findByName("John Doe").consume({
			println it
			person = it
			latch.countDown()
		} as Consumer<Person>).resolve()
		latch.await(1, TimeUnit.SECONDS)

		then: "entity should have a name property"
		person?.name == "John Doe"

	}

}

@Configuration
@EnableMongoRepositories(basePackages = ["reactor.data.spring.test"])
@EnableComposableRepositories(basePackages = ["reactor.data.spring.test"])
class SpecConfig {

	@Bean
	Environment reactorEnv() {
		return new Environment()
	}

	@Bean
	MongoTemplate mongoTemplate() {
		return new MongoTemplate(new Mongo(), "reactor")
	}

}
