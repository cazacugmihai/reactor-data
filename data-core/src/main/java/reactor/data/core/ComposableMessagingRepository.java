package reactor.data.core;

import reactor.core.composable.Promise;
import reactor.core.composable.Stream;

import java.io.Serializable;

/**
 * A {@code ComposableMessagingRepository} is a way to send and receive simple messages backed by different
 * implementations, depending on the needs of the application.
 *
 * @author Jon Brisbin
 */
public interface ComposableMessagingRepository<V, K extends Serializable> {

	/**
	 * Send the given message using the given routing key.
	 *
	 * @param key
	 * 		The routing key to use.
	 * @param message
	 * 		The message to send.
	 *
	 * @return a {@link reactor.core.composable.Promise} that will be fulfilled when the messages has been transmitted.
	 */
	Promise<Void> send(K key, V message);

	/**
	 * Receive messages sent to the given routing key.
	 *
	 * @param key
	 * 		The routing key to use.
	 *
	 * @return a {@link reactor.core.composable.Stream} that will be populated by messages sent to the given routing key.
	 */
	Stream<V> receive(K key);

}
