package reactor.data.spring.redis;

import com.lambdaworks.redis.RedisClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.Lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jon Brisbin
 */
public class RedisClientFactoryBean implements FactoryBean<RedisClient>, Lifecycle {

	private final RedisClient redisClient;
	private final AtomicBoolean running = new AtomicBoolean();

	public RedisClientFactoryBean(String host) {
		this.redisClient = new RedisClient(host);
	}

	public RedisClientFactoryBean(String host, int port) {
		this.redisClient = new RedisClient(host, port);
	}

	@Override
	public RedisClient getObject() throws Exception {
		return redisClient;
	}

	@Override
	public Class<?> getObjectType() {
		return RedisClient.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void start() {
		this.running.set(true);
	}

	@Override
	public void stop() {
		redisClient.shutdown();
		this.running.set(false);
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

}
