package reactor.data.reactor;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.DeferredPromiseSpec;
import reactor.core.composable.spec.DeferredStreamSpec;
import reactor.core.composable.spec.Promises;
import reactor.core.composable.spec.Streams;
import reactor.core.spec.Reactors;
import reactor.data.core.ComposableEventRepository;
import reactor.event.Event;
import reactor.event.selector.Selector;
import reactor.function.Consumer;

import java.io.Serializable;

import static reactor.event.selector.Selectors.$;

/**
 * @author Jon Brisbin
 */
public class ReactorComposableEventRepository<V, K extends Serializable>
		implements ComposableEventRepository<V, K> {

	private final Environment env;
	private final String      dispatcher;
	private final Reactor     reactor;

	public ReactorComposableEventRepository(Environment env,
	                                        String dispatcher) {
		this.env = env;
		this.dispatcher = dispatcher;
		this.reactor = Reactors.reactor().env(env).dispatcher(dispatcher).get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<Event<V>> receive(K key) {
		if(key instanceof String && ((String)key).contains("*")) {
			key = (K)((String)key).replace("\\*", "(.*)");
		}
		return receive($(key));
	}

	@Override
	public Stream<Event<V>> receive(Selector sel) {
		DeferredStreamSpec<Event<V>> spec = Streams.<Event<V>>defer().env(env);
		if(null != dispatcher && "sync".equals(dispatcher)) {
			spec.synchronousDispatcher();
		}

		Deferred<Event<V>, Stream<Event<V>>> d = spec.get();
		reactor.on(sel, d);
		return d.compose();
	}

	@Override
	public Promise<Void> send(K key, Event<V> message) {
		DeferredPromiseSpec<Void> spec = Promises.<Void>defer().env(env);
		if(null != dispatcher && "sync".equals(dispatcher)) {
			spec.synchronousDispatcher();
		}

		final Deferred<Void, Promise<Void>> d = spec.get();
		reactor.notify(key, message, new Consumer<Event<V>>() {
			@Override
			public void accept(Event<V> ev) {
				d.accept((Void)null);
			}
		});
		return d.compose();
	}

}
