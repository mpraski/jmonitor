package com.mpraski.jmonitor.util;

import java.util.Comparator;
import java.util.Objects;

public final class Pair<K extends Comparable<? super K>, V> implements Comparable<Pair<K, V>> {
	private final K key;
	private final V value;

	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!(o instanceof Pair<?, ?>)) {
			return false;
		}

		Pair<?, ?> c = (Pair<?, ?>) o;

		return Objects.equals(key, c.getKey()) && Objects.equals(value, c.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(key) ^ Objects.hashCode(value);
	}

	@Override
	public int compareTo(Pair<K, V> o) {
		return Comparator.comparing(Pair<K, V>::getKey).compare(this, o);
	}

	@Override
	public String toString() {
		return '{' + Objects.toString(key, "null") + '=' + Objects.toString(value, "null") + '}';
	}
}
