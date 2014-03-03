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

import reactor.data.util.DataUtils;
import reactor.util.Assert;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Jon Brisbin
 */
public class OrderedAtomicList<T> extends AbstractList<T> {

	private static final AtomicIntegerFieldUpdater<OrderedAtomicList> SIZE_UPD
			= AtomicIntegerFieldUpdater.newUpdater(OrderedAtomicList.class, "size");

	private static final Field REF_ARRAY_OBJECTS;
	private static final long  REF_ARRAY_OFFSET;

	static {
		try {
			REF_ARRAY_OBJECTS = AtomicReferenceArray.class.getDeclaredField("array");
			REF_ARRAY_OBJECTS.setAccessible(true);
			REF_ARRAY_OFFSET = UnsafeUtils.getUnsafe().objectFieldOffset(REF_ARRAY_OBJECTS);
		} catch(NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}

	private final ReentrantLock tableLock = new ReentrantLock();

	private volatile int[]                   hashes = new int[2];
	private volatile AtomicReferenceArray<T> values = new AtomicReferenceArray<T>(2);
	private volatile int                     size   = 0;

	public OrderedAtomicList() {
	}

	public OrderedAtomicList(Collection<T> values) {
		int size = DataUtils.nextClosestPowerOf2(values.size());
		this.values = new AtomicReferenceArray<T>(size);
		this.hashes = new int[size];
		for(T obj : values) { add(obj); }
	}

	public OrderedAtomicList(int size) {
		size = DataUtils.nextClosestPowerOf2(size);
		this.values = new AtomicReferenceArray<T>(size);
		this.hashes = new int[size];
	}

	@Override
	public boolean contains(Object o) {
		if(null == o) { return false; }
		return indexOf(o) > -1;
	}

	@Override
	public int indexOf(Object o) {
		try {
			return Arrays.binarySearch(hashes, 0, size(), o.hashCode());
		} catch(IllegalArgumentException ignored) {
			return -1;
		} catch(ArrayIndexOutOfBoundsException ignored) {
			return -2;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void add(int index, T element) {
		Assert.notNull(element, "OrderedAtomicList does not accept null elements");

		while(index >= values.length()) { expand(); }

		int hashCode = element.hashCode();
		values.set(index, element);
		hashes[index] = hashCode;
		if(index >= size()) { SIZE_UPD.set(this, index + 1); }

		sort();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(int index) {
		return values.get(index);
	}

	@Override
	public T remove(int index) {
		if(index >= size()) { return null; }

		T oldVal = values.get(index);
		if(!values.compareAndSet(index, oldVal, null)) {
			return oldVal;
		}
		SIZE_UPD.decrementAndGet(this);

		sort();
		return oldVal;
	}

	@Override
	public int size() {
		return SIZE_UPD.get(this);
	}

	@SuppressWarnings("unchecked")
	public T[] values() {
		return (T[])UnsafeUtils.getUnsafe().getObject(OrderedAtomicList.this.values, REF_ARRAY_OFFSET);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<T> iterator() {
		final T[] values = (T[])UnsafeUtils.getUnsafe().getObject(OrderedAtomicList.this.values, REF_ARRAY_OFFSET);
		return new ArrayIterator<T>(values);
	}

	@SuppressWarnings("unchecked")
	public OrderedAtomicList<T> sort() {
		tableLock.lock();
		try {
			final T[] values = (T[])UnsafeUtils.getUnsafe().getObject(this.values, REF_ARRAY_OFFSET);
			Arrays.sort(values, 0, size());
			T[] newValues = (T[])new Object[size()];
			int[] newHashes = new int[newValues.length];
			int nextIdx = 0;
			for(T obj : values) {
				if(null == obj) { continue; }
				newHashes[nextIdx] = obj.hashCode();
				newValues[nextIdx] = obj;
				nextIdx++;
			}

			this.values = new AtomicReferenceArray<T>(newValues);
			this.hashes = newHashes;
			SIZE_UPD.set(this, nextIdx);
		} finally {
			tableLock.unlock();
			return this;
		}
	}

	@SuppressWarnings("unchecked")
	private AtomicReferenceArray<T> expand() {
		tableLock.lock();
		try {
			if(size() < values.length()) { return values; }

			int oldLen = values.length();
			int newLen = oldLen * 2;

			T[] oldValues = (T[])UnsafeUtils.getUnsafe().getObject(values, REF_ARRAY_OFFSET);
			AtomicReferenceArray<T> newValues = new AtomicReferenceArray<T>(Arrays.copyOf(oldValues, newLen));
			this.hashes = Arrays.copyOf(this.hashes, newLen);

			return (this.values = newValues);
		} finally {
			tableLock.unlock();
		}
	}

}
