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

import reactor.core.Observable;
import reactor.core.composable.Composable;
import reactor.core.composable.Deferred;
import reactor.event.Event;
import reactor.function.Function;

import java.io.Serializable;
import java.util.*;

/**
 * Implementation of {@link java.util.Map} that internally uses a {@link reactor.data.core.collection.OrderedAtomicList}
 * to manage the list of key-value entries stored inside this map.
 *
 * @author Jon Brisbin
 */
public class ObservableMap<K, V> extends AbstractMap<K, V> implements Serializable {

	private static final long serialVersionUID = -2972549238690433871L;

	private final OrderedAtomicList<ObservableEntry<K, V>> entries = new OrderedAtomicList<>();

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
		K k = (K)key;
		int idx = entries.indexOf(k);
		if(idx < 0) {
			if(null != defaultValueProvider) {
				V v = defaultValueProvider.apply(k);
				if(null != v) {
					ObservableEntry<K, V> e = new ObservableEntry<>(k, v);
					entries.add(e);
					notifyValue(e);
					return true;
				}
			}
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		K k = (K)key;
		int idx = entries.indexOf(k);
		if(idx >= 0) {
			ObservableEntry<K, V> e = entries.get(idx);
			V v = e.getValue();
			e.setValue(null);
			entries.remove(idx);
			notifyValue(e);
			return v;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		K k = (K)key;
		int idx = entries.indexOf(k);
		if(idx >= 0) {
			return entries.get(idx).getValue();
		}
		if(null != defaultValueProvider) {
			V v = defaultValueProvider.apply(k);
			ObservableEntry<K, V> e = new ObservableEntry<>(k, v);
			entries.add(e);
			notifyValue(e);
			return v;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
		int idx = entries.indexOf(key);
		ObservableEntry<K, V> e;
		V oldVal = null;
		if(idx >= 0) {
			e = entries.get(idx);
			oldVal = e.getValue();
			e.setValue(value);
		} else {
			e = new ObservableEntry<>(key, value);
			entries.add(e);
		}
		if(null != e) {
			notifyValue(e);
		}
		return oldVal;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for(K key : m.keySet()) {
			put(key, m.get(key));
		}
	}

	@Override
	public int size() {
		return entries.size();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySet<>();
	}

	@Override
	public Set<K> keySet() {
		return new AbstractSet<K>() {
			@Override
			public Iterator<K> iterator() {
				return new KeyIterator<>(entries.iterator());
			}

			@Override
			public int size() {
				return entries.size();
			}
		};
	}

	@Override
	public Collection<V> values() {
		List<V> values = new ArrayList<>();
		for(ObservableEntry<K, V> entry : entries) {
			values.add(entry.getValue());
		}
		return values;
	}

	private void notifyValue(ObservableEntry<K, V> entry) {
		if(null != observable) {
			observable.notify(entry.getKey(), entry);
		}
		if(null != deferred) {
			deferred.accept(entry);
		}
	}

	private class EntrySet<K, V> extends AbstractSet<Entry<K, V>> {
		@SuppressWarnings("unchecked")
		@Override
		public Iterator<Entry<K, V>> iterator() {
			return ((List)entries).iterator();
		}

		@Override
		public int size() {
			return entries.size();
		}
	}

	private static class KeyIterator<K, V> implements Iterator<K> {
		private final Iterator<ObservableEntry<K, V>> keys;

		private KeyIterator(Iterator<ObservableEntry<K, V>> keys) {
			this.keys = keys;
		}

		@Override
		public boolean hasNext() {
			return keys.hasNext();
		}

		@Override
		public K next() {
			return keys.next().getKey();
		}

		@Override
		public void remove() {
			keys.remove();
		}
	}

	private static class ObservableEntry<K, V> extends Event<V> implements Entry<K, V>,
	                                                                       Comparable<ObservableEntry<K, V>> {
		private final int hashCode;

		private ObservableEntry(K key, V value) {
			super(value);
			setKey(key);
			this.hashCode = key.hashCode();
		}

		@SuppressWarnings("unchecked")
		@Override
		public K getKey() {
			return (K)super.getKey();
		}

		@Override
		public V getValue() {
			return super.getData();
		}

		@Override
		public V setValue(V value) {
			V oldVal = super.getData();
			super.setData(value);
			return oldVal;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public int compareTo(ObservableEntry<K, V> o) {
			return Integer.compare(hashCode, o.hashCode);
		}
	}

}
