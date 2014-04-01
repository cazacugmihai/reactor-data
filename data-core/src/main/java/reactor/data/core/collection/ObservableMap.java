/*
 * Copyright (c) 2011-2014 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.data.core.collection;

import com.gs.collections.api.map.MutableMap;
import com.gs.collections.impl.block.function.checked.CheckedFunction0;
import com.gs.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import reactor.core.Observable;
import reactor.core.composable.Composable;
import reactor.core.composable.Deferred;
import reactor.event.Event;
import reactor.function.Function;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link java.util.Map} that provides event sourcing for updates to the Map.
 *
 * @author Jon Brisbin
 */
public class ObservableMap<K, V> extends AbstractMap<K, V> implements Serializable {

	private static final long serialVersionUID = -2972549238690433871L;

	private final MutableMap<K, V> entries = new ConcurrentHashMapUnsafe<>();

	private final Observable                                               observable;
	private final Deferred<Entry<K, V>, ? extends Composable<Entry<K, V>>> deferred;
	private final Function<K, V>                                           defaultValueProvider;

	/**
	 * Create an {@literal ObservableMap}, reporting updates to the given {@link reactor.core.Observable} or {@link
	 * reactor.core.composable.Deferred}, and optionally providing a default value for a given key using the given {@link
	 * reactor.function.Function}.
	 *
	 * @param observable
	 * 		the {@link reactor.core.Observable} to report updates to. may be {@literal null}
	 * @param deferred
	 * 		the {@link reactor.core.composable.Deferred} to report updates to. may be {@literal null}
	 * @param defaultValueProvider
	 * 		the {@literal Function} to invoke to provide a default value for a given key
	 */
	public ObservableMap(Observable observable,
	                     Deferred<Entry<K, V>, ? extends Composable<Entry<K, V>>> deferred,
	                     Function<K, V> defaultValueProvider) {
		this.observable = observable;
		this.deferred = deferred;
		this.defaultValueProvider = defaultValueProvider;
	}

	/**
	 * Get the {@link reactor.core.Observable} being used.
	 *
	 * @return the {@link reactor.core.Observable}
	 */
	public Observable getObservable() {
		return observable;
	}

	/**
	 * Get the {@link reactor.core.composable.Deferred} being used.
	 *
	 * @return the {@link reactor.core.composable.Deferred}
	 */
	public Deferred<Entry<K, V>, ? extends Composable<Entry<K, V>>> getDeferred() {
		return deferred;
	}

	@Override
	public void clear() {
		entries.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsKey(Object key) {
		return null != get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		return entries.remove(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		final K k = (K) key;
		return entries.getIfAbsentPut(k, new CheckedFunction0<V>() {
			@Override
			public V safeValue() throws Exception {
				V val = null;
				if (null != defaultValueProvider && null != (val = defaultValueProvider.apply(k))) {
					notifyValue(new ObservableEntry<>(k, val, entries));
				}
				return val;
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
		V oldVal = entries.replace(key, value);
		if (null == oldVal || (oldVal != value || !oldVal.equals(value))) {
			notifyValue(new ObservableEntry<>(key, value, entries));
		}
		return oldVal;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		entries.putAll(m);
	}

	@Override
	public int size() {
		return entries.size();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return entries.entrySet();
	}

	@Override
	public Set<K> keySet() {
		return entries.keySet();
	}

	@Override
	public Collection<V> values() {
		return entries.values();
	}

	private void notifyValue(ObservableEntry<K, V> entry) {
		if (null != observable) {
			observable.notify(entry.getKey(), entry);
		}
		if (null != deferred) {
			deferred.accept(entry);
		}
	}

	private static class ObservableEntry<K, V> extends Event<V> implements Entry<K, V>,
	                                                                       Comparable<ObservableEntry<K, V>> {
		private final MutableMap<K, V> entries;

		private ObservableEntry(K key, V value, MutableMap<K, V> entries) {
			super(value);
			setKey(key);
			this.entries = entries;
		}

		@SuppressWarnings("unchecked")
		@Override
		public K getKey() {
			return (K) super.getKey();
		}

		@Override
		public V getValue() {
			return super.getData();
		}

		@Override
		public V setValue(V value) {
			V oldVal = super.getData();
			super.setData(value);
			while (!entries.replace(getKey(), oldVal, value)) {
				oldVal = entries.get(getKey());
			}
			return oldVal;
		}

		@Override
		public int hashCode() {
			return getKey().hashCode();
		}

		@Override
		public int compareTo(ObservableEntry<K, V> o) {
			return Integer.compare(getKey().hashCode(), o.getKey().hashCode());
		}
	}

}
