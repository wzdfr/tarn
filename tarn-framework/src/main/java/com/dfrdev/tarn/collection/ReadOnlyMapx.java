package com.dfrdev.tarn.collection;


import com.dfrdev.tarn.core.TarnException;

import java.util.*;

public class ReadOnlyMapx<K, V> extends Mapx<K, V> {
    private static final long serialVersionUID = 1L;
    Map<K, V> data;
    volatile transient Set<K> keySet = null;
    volatile transient Collection<V> values = null;
    transient ReadOnlyMapx<K, V>.EntrySet entrySet = null;

    public ReadOnlyMapx(Map<K, V> data) {
        if (data == null) {
            throw new NullPointerException();
        }
        this.data = data;
    }

    public int size() {
        return this.data.size();
    }

    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.data.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return this.data.containsValue(value);
    }

    public V get(Object key) {
        return this.data.get(key);
    }

    public V put(K key, V value) {
        throw new MapReadOnlyException();
    }

    public V remove(Object key) {
        throw new MapReadOnlyException();
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        throw new MapReadOnlyException();
    }

    public void clear() {
        throw new MapReadOnlyException();
    }

    public Set<K> keySet() {
        return this.keySet == null ? (this.keySet = new KeySet(this.data.keySet())) : this.keySet;
    }

    public Collection<V> values() {
        return this.values == null ? (this.values = new Values(this.data.values())) : this.values;
    }

    public Set entrySet() {
        return this.entrySet == null ? (this.entrySet = new EntrySet(this.data.entrySet())) : this.entrySet;
    }

    private class EntryIterator
            implements Iterator<Entry<K, V>> {
        Iterator<Entry<K, V>> data;

        public EntryIterator(ReadOnlyMapx<K, V> kvReadOnlyMapx, Iterator<Entry<K, V>> iterator) {
            this.data = data;
        }

        public Entry<K, V> next() {
            return (Entry) this.data.next();
        }

        public boolean hasNext() {
            return this.data.hasNext();
        }

        public void remove() {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {
        Set<Entry<K, V>> data;

        public EntrySet(Set<Map.Entry<K, V>> entries) {
            this.data = data;
        }

        public Iterator<Entry<K, V>> iterator() {
            return new ReadOnlyMapx.EntryIterator(ReadOnlyMapx.this, this.data.iterator());
        }

        public int size() {
            return this.data.size();
        }
    }

    private class KeyIterator
            implements Iterator<K> {
        Iterator<K> data;

        public KeyIterator(ReadOnlyMapx<K, V> kvReadOnlyMapx, Iterator<K> iterator) {
            this.data = data;
        }

        public K next() {
            return this.data.next();
        }

        public boolean hasNext() {
            return this.data.hasNext();
        }

        public void remove() {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }
    }

    private class KeySet extends AbstractSet<K> {
        Set<K> data;

        public KeySet(Set<K> ks) {
            this.data = data;
        }

        public Iterator<K> iterator() {
            return new ReadOnlyMapx.KeyIterator(ReadOnlyMapx.this, this.data.iterator());
        }

        public int size() {
            return this.data.size();
        }
    }

    public static class MapReadOnlyException extends TarnException {
        private static final long serialVersionUID = 1L;

        public MapReadOnlyException() {
            super("");
        }
    }

    private class ValueIterator
            implements Iterator<V> {
        Iterator<V> data;

        public ValueIterator(ReadOnlyMapx<K, V> kvReadOnlyMapx, Iterator<V> iterator) {
            this.data = data;
        }

        public V next() {
            return this.data.next();
        }

        public boolean hasNext() {
            return this.data.hasNext();
        }

        public void remove() {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }
    }

    private class Values
            implements Collection<V> {
        Collection<V> data;

        public Values(Collection<V> values) {
            this.data = data;
        }

        public Iterator<V> iterator() {
            return new ReadOnlyMapx.ValueIterator(ReadOnlyMapx.this, this.data.iterator());
        }

        public int size() {
            return this.data.size();
        }

        public boolean isEmpty() {
            return this.data.isEmpty();
        }

        public boolean contains(Object o) {
            return this.data.contains(o);
        }

        public Object[] toArray() {
            return this.data.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return this.data.toArray(a);
        }

        public boolean add(V o) {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }

        public boolean remove(Object o) {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }

        public boolean containsAll(Collection<?> c) {
            return this.data.containsAll(c);
        }

        public boolean addAll(Collection<? extends V> c) {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }

        public boolean removeAll(Collection<?> c) {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }

        public boolean retainAll(Collection<?> c) {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }

        public void clear() {
            throw new ReadOnlyMapx.MapReadOnlyException();
        }
    }
}

