package reactor.data.spring.commons;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.ReflectionUtils;
import reactor.core.Environment;
import reactor.core.composable.Deferred;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.Streams;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.springframework.util.ReflectionUtils.doWithMethods;

/**
 * @author Jon Brisbin
 */
public class SpringDataRepositoryQueryMethodMethodInterceptor<V, K extends Serializable> implements MethodInterceptor {

	private final Environment                              env;
	private final String                                   dispatcher;
	private final Executor                                 executor;
	private final SpringDataComposableCrudRepository<V, K> compRepo;
	private final Map<String, Method>     crudMethods  = new HashMap<>();
	private final Map<String, Method>     queryMethods = new HashMap<>();
	private final Map<String, Class<?>[]> paramTypes   = new HashMap<>();

	public SpringDataRepositoryQueryMethodMethodInterceptor(
			Environment env,
			String dispatcher,
			Executor executor,
			Class<?> compRepoType,
			SpringDataComposableCrudRepository<V, K> compRepo
	) {
		this.env = env;
		this.dispatcher = dispatcher;
		this.executor = executor;
		this.compRepo = compRepo;

		doWithMethods(
				compRepoType,
				new ReflectionUtils.MethodCallback() {
					@Override
					public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
						String name = method.toGenericString();
						Class<?>[] paramTypes = method.getParameterTypes();
						SpringDataRepositoryQueryMethodMethodInterceptor.this.paramTypes.put(name, paramTypes);
					}
				},
				new ReflectionUtils.MethodFilter() {
					@Override
					public boolean matches(Method method) {
						return Object.class != method.getDeclaringClass() && !method.getName().contains("$");
					}
				}
		);
	}

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		String name = invocation.getMethod().toGenericString();
		Class<?>[] paramTypes = this.paramTypes.get(name);

		try {
			Method m;
			if(null == (m = crudMethods.get(name))) {
				if(null != (m = invocation.getThis()
				                          .getClass()
				                          .getDeclaredMethod(invocation.getMethod().getName(), paramTypes))) {
					crudMethods.put(name, m);
				}
			}
			if(null != m) {
				return m.invoke(invocation.getThis(), invocation.getArguments());
			}
		} catch(Exception e) {
			if(NoSuchMethodException.class.isAssignableFrom(e.getClass())) {
				// this is probably a finder method
				Method m;
				if(null == (m = queryMethods.get(name))) {
					if(null != (m = compRepo.getDelegateRepository()
					                        .getClass()
					                        .getDeclaredMethod(invocation.getMethod().getName(), paramTypes))) {
						queryMethods.put(name, m);
					}
				}
				if(null != m) {
					final Deferred<Object, Stream<Object>> d = Streams.<Object>defer()
					                                                  .env(env)
					                                                  .dispatcher(dispatcher)
					                                                  .get();

					final Method queryMethod = m;
					executor.execute(new Runnable() {
						@Override
						public void run() {
							Object returnVal = ReflectionUtils.invokeMethod(queryMethod,
							                                                compRepo.getDelegateRepository(),
							                                                invocation.getArguments());
							d.accept(returnVal);
						}
					});

					return d.compose();
				}
			}

			throw e;
		}

		throw new NoSuchMethodException(name);
	}
}
