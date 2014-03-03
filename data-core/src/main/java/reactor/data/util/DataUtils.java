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

package reactor.data.util;

/**
 * @author Jon Brisbin
 */
public abstract class DataUtils {

	protected DataUtils() {
	}

	/**
	 * Find a power of 2 size nearest the requested size. Modified from {@link java.util.concurrent.ConcurrentHashMap}.
	 *
	 * @param size
	 *
	 * @return
	 */
	public static int nextClosestPowerOf2(int size) {
		int n = size - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return (n < 0 ? 2 : n + 1);
	}

}
