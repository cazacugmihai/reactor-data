package reactor.data.core;

import reactor.core.composable.Stream;
import reactor.event.Event;
import reactor.event.selector.Selector;

import java.io.Serializable;

/**
 * @author Jon Brisbin
 */
public interface ComposableEventRepository<V, K extends Serializable>
		extends ComposableMessagingRepository<Event<V>, K> {

	Stream<Event<V>> receive(Selector sel);

}
