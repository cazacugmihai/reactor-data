package reactor.data;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.dispatch.SynchronousDispatcher;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jon Brisbin
 */
public abstract class AbstractObservableTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	protected Environment env;
	protected Reactor     reactor;
	protected AtomicLong  counter;

	private long   start;
	private long   end;
	private double elapsed;
	private long   throughput;


	@Before
	public void setup() {
		counter = new AtomicLong(0);
		env = new Environment();
		reactor = Reactors.reactor(env, new SynchronousDispatcher());
	}

	protected abstract long getTimeout();

	protected void startThroughputTest(String type) {
		log.info("Starting {} throughput test...", type);
		start = System.currentTimeMillis();
	}

	protected void stopThroughputTest(String type) {
		end = System.currentTimeMillis();
		elapsed = end - start;
		throughput = (long)(counter.get() / (elapsed / 1000));

		log.info("{} throughput: {}/s in {}ms", type, throughput, (int)elapsed);
	}

	protected boolean withinTimeout() {
		return System.currentTimeMillis() - start < getTimeout();
	}

}
