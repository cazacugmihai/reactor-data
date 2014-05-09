package reactor.data.core.collection;

import org.junit.Before;
import org.junit.Test;
import reactor.data.AbstractObservableTests;
import reactor.event.Event;
import reactor.event.dispatch.SynchronousDispatcher;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.function.Predicate;
import reactor.rx.Stream;
import reactor.rx.spec.Streams;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static reactor.event.selector.Selectors.$;

/**
 * @author Jon Brisbin
 */
public class ObservableMapTests extends AbstractObservableTests {

	static final int ITEMS = 8;

	private Object[] keys   = new Object[ITEMS];
	private Object[] values = new Object[ITEMS];
	private ObservableMap<Object, Object>                                          map;
	private Stream<Map.Entry<Object, Object>> deferred;

	@Before
	public void setup() {
		super.setup();

		deferred = Streams.defer(env, new SynchronousDispatcher());
		map = new ObservableMap<>(reactor,
		                          deferred,
		                          null);

		for (int i = 0; i < keys.length; i++) {
			final int idx = i;
			keys[i] = "key[" + idx + "]";
			values[i] = "value[" + idx + "]";

			map.put(keys[i], values[i]);
		}
	}

	@Override
	protected long getTimeout() {
		return 1000;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void exposesEventsOnUpdates() {
		Consumer<Event<String>> consumer = mock(Consumer.class);

		reactor.on($("test"), consumer);
		map.put("test", "Hello World!");
		map.put("test", "Hello World!");

		verify(consumer, atLeast(2)).accept(any(Event.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void promiseIsFulfilledWhenValueIsSet() {
		Consumer<Map.Entry<Object, Object>> consumer = mock(Consumer.class);

		deferred
		        .filter(new Predicate<Map.Entry<Object, Object>>() {
			        @Override
			        public boolean test(Map.Entry<Object, Object> entry) {
				        return "test".equals(entry.getKey());
			        }
		        })
		        .consume(consumer);
		map.put("test", "Hello World!");

		verify(consumer).accept(any(Map.Entry.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void defaultIsProvidedWhenKeyDoesntExist() {
		String key = "not found";

		Function<String, String> fn = mock(Function.class);
		when(fn.apply(key)).thenReturn("Hello World!");

		Map<String, String> m = new ObservableMap<>(null, null, fn);
		String s1 = m.get(key);
		String s2 = m.get(key);

		verify(fn, atMost(1)).apply(key);
		assertThat("Default was returned the first time", s1, is("Hello World!"));
		assertThat("Value was stored for second time", s2, is("Hello World!"));
	}

	@Test
	public void iterationComparedToHashMap() throws InterruptedException {
		final int items = 512;

		String[] keys = new String[items];
		String[] values = new String[items];

		Map<String, String> om = new ObservableMap<>(null, null, null);
		Map<String, String> hm = new HashMap<>();

		for (int i = 0; i < items; i++) {
			keys[i] = "key[" + i + "]";
			values[i] = "value[" + i + "]";

			om.put(keys[i], values[i]);
			hm.put(keys[i], values[i]);
		}
		String testType = "ObservableMap iteration horserace";
		startThroughputTest(testType);
		while (withinTimeout()) {
			for (Map.Entry<String, String> entry : om.entrySet()) {
				assert null != entry.getValue();
			}
			counter.addAndGet(items);
		}
		stopThroughputTest(testType);

		Thread.sleep(3000);

		testType = "HashMap iteration horserace";
		startThroughputTest(testType);
		while (withinTimeout()) {
			for (Map.Entry<String, String> entry : hm.entrySet()) {
				assert null != entry.getValue();
			}
			counter.addAndGet(items);
		}
		stopThroughputTest(testType);
	}

	@Test
	public void throughputComparedToHashMap() throws InterruptedException {
		final int items = 512;

		String[] keys = new String[items];
		String[] values = new String[items];

		Map<String, String> om = new ObservableMap<>(null, null, null);
		Map<String, String> hm = new HashMap<>();

		for (int i = 0; i < items; i++) {
			keys[i] = "key[" + i + "]";
			values[i] = "value[" + i + "]";
			hm.put(keys[i], values[i]);
		}
		om.putAll(hm);

		String testType = "ObservableMap get horserace";
		startThroughputTest(testType);
		while (withinTimeout()) {
			iterateFully(keys, values, om);
		}
		stopThroughputTest(testType);

		Thread.sleep(3000);

		testType = "HashMap get horserace";
		startThroughputTest(testType);
		while (withinTimeout()) {
			iterateFully(keys, values, hm);
		}
		stopThroughputTest(testType);
	}

	private void iterateFully(String[] keys, String[] values, Map<String, String> m) {
		int i = 0;
		for (String key : keys) {
			int idx = i++;
			String s = m.get(key);
			if (!values[idx].equals(s)) {
				throw new IllegalArgumentException(key + "=" + s + " should be " + key + "=" + values[idx]);
			}
		}
		counter.addAndGet(keys.length);
	}

}
