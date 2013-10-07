package reactor.data.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to denote what implementation of {@code ComposableRepository} should back this interface.
 *
 * @author Jon Brisbin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Provider {

	/**
	 * String identifier for the type of implementation desired to back this composable repository type.
	 *
	 * @return Implementation identifier.
	 */
	String value();

}
