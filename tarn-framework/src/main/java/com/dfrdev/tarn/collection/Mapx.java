package com.dfrdev.tarn.collection;


import com.dfrdev.tarn.util.date.DateUtil;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.classloader.Primitives;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class Mapx<K, V> extends AbstractMap<K, V>
        implements Map<K, V>, Cloneable, Serializable {
    static final long serialVersionUID = 201404091412L;
    static final Object NULL_KEY = new Object();
    static final Lock SHAM_LOCK = new ShamLock();
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final int MAXIMUM_CAPACITY = 1073741824;
    static final float DEFAULT_LOAD_FACTOR = 0.75F;
    volatile transient Set<K> keySet = null;
    volatile transient Collection<V> values = null;
    transient Set<Map.Entry<K, V>> entrySet = null;
    transient KeyNotFoundEventListener<K, V> keyNotFoundEventListener = null;
    transient Entry<K, V>[] table;
    transient int size;
    transient AtomicInteger modCount = new AtomicInteger();
    transient Entry<K, V> header;
    transient Lock rlock = SHAM_LOCK;
    transient Lock wlock = SHAM_LOCK;
    transient int threshold;
    transient int accessCount = -1;

    public Mapx() {
        this.threshold = 12;
        this.table = new Entry[16];
        init();
    }

    public Mapx(int initialCapacity) {
        this(initialCapacity, false);
    }

    public Mapx(boolean threadSafe) {
        this();
        if (threadSafe) {
            ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
            this.rlock = rwlock.readLock();
            this.wlock = rwlock.writeLock();
        }
    }

    public Mapx(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / 0.75F) + 1, 16), false);
        putAllForCreate(m);
    }

    public Mapx(int initialCapacity, boolean threadSafe) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (initialCapacity > 1073741824) {
            initialCapacity = 1073741824;
        }
        int capacity = 1;
        while (capacity < initialCapacity) {
            capacity <<= 1;
        }

        this.threshold = (int) (capacity * 0.75F);
        this.table = new Entry[16];
        if (threadSafe) {
            ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
            this.rlock = rwlock.readLock();
            this.wlock = rwlock.writeLock();
        }
        init();
    }

    void init() {
        this.header = new Entry(-1, null, null, null);
        this.header.before = (this.header.after = this.header);
    }

    <T> T maskKey(T key) {
        return key == null ? (T) NULL_KEY : key;
    }

    static int hash(int h) {
        h += (h << 9 ^ 0xFFFFFFFF);
        h ^= h >>> 14;
        h += (h << 4);
        h ^= h >>> 10;
        return h;
    }

    int hash(Object key) {
        return hash(key.hashCode());
    }

    boolean eq(Object x, int hash, Entry<K, V> e) {
        return (x == e.key) || (x.equals(e.key));
    }

    static int indexFor(int h, int length) {
        return h & length - 1;
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    Entry<K, V> getEntry(Object key) {
        key = maskKey(key);
        int hash = hash(key);
        for (Entry e = this.table[indexFor(hash, this.table.length)]; e != null; e = e.next) {
            if ((e.hash == hash) && (eq(key, hash, e))) {
                return e;
            }
        }

        this.rlock.lock();
        try {
            for (Entry e = this.table[indexFor(hash, this.table.length)]; e != null; e = e.next)
                if ((e.hash == hash) && (eq(key, hash, e)))
                    return e;
        } finally {
            this.rlock.unlock();
        }
        this.rlock.unlock();

        return null;
    }





    public V put(K key, V value) {
        if (key == null) {
            return putForNullKey(value);
        }
        key = maskKey(key);
        int hash = hash(key);
        int i = indexFor(hash, this.table.length);

        this.wlock.lock();
        try {
            for (Entry e = this.table[i]; e != null; e = e.next) {
                if ((e.hash == hash) && (eq(key, hash, e))) {
                    e.value = value;
                    return (V) e.value;
                }
            }
            this.modCount.incrementAndGet();
            addEntry(hash, key, value, i);
            return null;
        } finally {
            this.wlock.unlock();
        }
    }

    V putForNullKey(V value) {
        int hash = hash(NULL_KEY);
        this.wlock.lock();
        try {
            int i = indexFor(hash, this.table.length);
            for (Entry e = this.table[i]; e != null; e = e.next) {
                if (e.key == NULL_KEY) {
                    e.value = value;
                    return (V) e.value;
                }
            }
            this.modCount.incrementAndGet();
            addEntry(hash, (K) NULL_KEY, value, i);
            return null;
        } finally {
            this.wlock.unlock();
        }
    }

    void putForCreate(K key, V value) {
        Object k = maskKey(key);
        int hash = hash(k);
        int i = indexFor(hash, this.table.length);
        for (Entry e = this.table[i]; e != null; e = e.next) {
            if ((e.hash == hash) && (eq(k, hash, e))) {
                e.value = value;
                return;
            }
        }
        createEntry(hash, (K) k, value, i);
    }

    void addEntry(int hash, K key, V value, int bucketIndex) {
        createEntry(hash, key, value, bucketIndex);
        Entry eldest = this.header.after;
        if (removeEldestEntry(eldest)) {
            removeEntryForKey(eldest.key);
        } else if (this.size >= this.threshold)
            resize(2 * this.table.length);
    }

    void createEntry(int hash, K key, V value, int bucketIndex) {
        Entry old = this.table[bucketIndex];
        Entry e = new Entry(hash, key, value, old);
        this.table[bucketIndex] = e;
        e.addBefore(this.header);
        this.size += 1;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return false;
    }

    void putAllForCreate(Map<? extends K, ? extends V> m) {
        for (Map.Entry e : m.entrySet())
            putForCreate((K) e.getKey(), (V) e.getValue());
    }

    void resize(int newCapacity) {
        Entry[] oldTable = this.table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == 1073741824) {
            this.threshold = 2147483647;
            return;
        }
        Entry[] newTable = new Entry[newCapacity];
        transfer(newTable);
        this.table = newTable;
        this.threshold = (int) (newCapacity * 0.75F);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        if (m == null) {
            return;
        }
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0) {
            return;
        }
        this.wlock.lock();
        try {
            int newCapacity;
            if (numKeysToBeAdded > this.threshold) {
                int targetCapacity = (int) (numKeysToBeAdded / 0.75F + 1.0F);
                if (targetCapacity > 1073741824) {
                    targetCapacity = 1073741824;
                }
                newCapacity = this.table.length;
                while (newCapacity < targetCapacity) {
                    newCapacity <<= 1;
                }
                if (newCapacity > this.table.length) {
                    resize(newCapacity);
                }
            }
            for (Map.Entry e : m.entrySet())
                put((K) e.getKey(), (V) e.getValue());
        } finally {
            this.wlock.unlock();
        }
    }

    public V remove(Object key) {
        Entry e = removeEntryForKey(key);
        return e == null ? null : (V) e.value;
    }

    Entry<K, V> removeEntryForKey(Object key) {
        Object k = maskKey(key);
        int hash = hash(key);
        this.wlock.lock();
        try {
            int i = indexFor(hash, this.table.length);
            Entry prev = this.table[i];
            Entry e = prev;
            Entry localEntry1;
            while (e != null) {
                Entry next = e.next;
                if ((e.hash == hash) && (eq(k, hash, e))) {
                    this.modCount.incrementAndGet();
                    this.size -= 1;
                    if (prev == e)
                        this.table[i] = next;
                    else {
                        prev.next = next;
                    }
                    e.recordRemoval(this);
                    return e;
                }
                prev = e;
                e = next;
            }
            return e;
        } finally {
            this.wlock.unlock();
        }
    }

    Entry<K, V> removeMapping(Object o) {
        if (!(o instanceof Map.Entry)) {
            return null;
        }
        Map.Entry entry = (Map.Entry) o;
        Object k = maskKey(entry.getKey());
        int hash = hash(k);
        this.wlock.lock();
        try {
            int i = indexFor(hash, this.table.length);
            Entry prev = this.table[i];
            Entry e = prev;
            Entry localEntry1;
            while (e != null) {
                Entry next = e.next;
                if ((e.hash == hash) && (e.equals(entry))) {
                    this.modCount.incrementAndGet();
                    this.size -= 1;
                    if (prev == e)
                        this.table[i] = next;
                    else {
                        prev.next = next;
                    }
                    e.recordRemoval(this);
                    return e;
                }
                prev = e;
                e = next;
            }
            return e;
        } finally {
            this.wlock.unlock();
        }
    }

    public Mapx<K, V> clone() {
        Mapx<K, V> result = null;
        try {
            result = (Mapx) super.clone();
        } catch (CloneNotSupportedException localCloneNotSupportedException) {
        }
        this.rlock.lock();
        try {
            result.table = new Entry[this.table.length];
            result.entrySet = null;
            result.modCount.set(0);
            result.size = 0;
            result.init();
            result.putAllForCreate(this);
            for (Map.Entry e : result.entrySet())
                if (e != null) {
                    Object v = e.getValue();
                    if (v != null) {
                        if ((v instanceof Mapx)) {
                            Mapx map = (Mapx) v;
                            map = map.clone();
                            e.setValue(map);
                        }
                    }
                }
            return result;
        } finally {
            this.rlock.unlock();
        }
    }

    public Set<K> keySet() {
        Set ks = this.keySet;
        return this.keySet = new KeySet();
    }

    public Collection<V> values() {
        Collection vs = this.values;
        return this.values = new Values();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set es = this.entrySet;
        return this.entrySet = new EntrySet();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        Iterator i = entrySet().iterator();
        s.defaultWriteObject();
        s.writeInt(this.table.length);
        s.writeInt(this.size);
        s.writeInt(this.accessCount != -1 ? 0 : 1);
        s.writeInt(this.rlock == SHAM_LOCK ? 0 : 1);
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        int numBuckets = s.readInt();
        this.table = new Entry[numBuckets];
        this.threshold = (int) (this.table.length * 0.75F);
        init();
        int size = s.readInt();
        this.accessCount = s.readInt();
        int shamFlag = s.readInt();
        this.modCount = new AtomicInteger();
        if (shamFlag == 0) {
            this.rlock = (this.wlock = SHAM_LOCK);
        } else {
            ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
            this.rlock = rwlock.readLock();
            this.wlock = rwlock.writeLock();
        }
        for (int i = 0; i < size; i++) {
            Object key = s.readObject();
            Object value = s.readObject();
            putForCreate((K) key, (V) value);
        }
    }

    void transfer(Entry<K, V>[] newTable) {
        int newCapacity = newTable.length;
        for (Entry e = this.header.after; e != this.header; e = e.after) {
            int index = indexFor(e.hash, newCapacity);
            e.next = newTable[index];
            newTable[index] = e;
        }
    }

    public boolean containsValue(Object value) {
        this.rlock.lock();
        try {
            if (value == null) {
                for (Entry e = this.header.after; e != this.header; e = e.after) {
                    if (e.value == null)
                        return true;
                }
            } else {
                for (Entry e = this.header.after; e != this.header; e = e.after) {
                    if (value.equals(e.value)) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            this.rlock.unlock();
        }
    }

    public V get(Object key) {
        key = maskKey(key);
        Entry e = getEntry(key);
        if (e == null) {
            if ((this.keyNotFoundEventListener != null) && (key != NULL_KEY)) {
                Object v = this.keyNotFoundEventListener.onKeyNotFound((K) key);
                put((K) key, (V) v);
                return (V) v;
            }
            return null;
        }
        if (this.accessCount != -1) {
            e.lastAccess = this.accessCount++;
        }
        return (V) e.value;
    }

    public void clear() {
        this.wlock.lock();
        try {
            this.modCount.incrementAndGet();
            Entry[] tab = this.table;
            for (int i = 0; i < tab.length; i++) {
                tab[i] = null;
            }
            this.size = 0;
            this.header.before = (this.header.after = this.header);
        } finally {
            this.wlock.unlock();
        }
    }

    public String getString(K key) {
        Object o = get(key);
        if (o == null) {
            return null;
        }
        return o.toString();
    }

    public boolean getBoolean(K key) {
        Object o = get(key);
        return Boolean.parseBoolean(o + "");
    }

    public int getInt(K key) throws Exception {
        Object o = get(key);
        return Primitives.getInteger(o);
    }

    public long getLong(K key) throws Exception {
        Object o = get(key);
        return Primitives.getLong(o);
    }

    public float getFloat(K key) throws Exception {
        Object o = get(key);
        return Primitives.getFloat(o);
    }

    public double getDouble(K key) throws Exception {
        Object o = get(key);
        return Primitives.getDouble(o);
    }

    public Date getDate(K key) {
        Object o = get(key);
        if ((o instanceof Date))
            return (Date) o;
        if (o != null) {
            try {
                return DateUtil.parseDateTime(o.toString());
            } catch (Exception e) {
                LogUtil.warn("日期解析失败: " + o.toString() + " -> " + e.getMessage());
            }
        }
        return null;
    }



    Iterator<K> newKeyIterator() {
        return new KeyIterator();
    }

    Iterator<V> newValueIterator() {
        return new ValueIterator();
    }

    Iterator<Map.Entry<K, V>> newEntryIterator() {
        return new EntryIterator();
    }

    @Deprecated
    public ArrayList<K> keyArray() {
        ArrayList list = new ArrayList();
        for (Map.Entry e : entrySet()) {
            list.add(e.getKey());
        }
        return list;
    }

    @Deprecated
    public ArrayList<V> valueArray() {
        ArrayList list = new ArrayList();
        for (Map.Entry e : entrySet()) {
            list.add(e.getValue());
        }
        return list;
    }

    public KeyNotFoundEventListener<K, V> getKeyNotFoundEventListener() {
        return this.keyNotFoundEventListener;
    }

    public void setKeyNotFoundEventListener(KeyNotFoundEventListener<K, V> keyNotFoundEventListener) {
        this.keyNotFoundEventListener = keyNotFoundEventListener;
    }

    abstract class AbstractIterator<T>
            implements Iterator<T> {
        Mapx.Entry<K, V> nextEntry = Mapx.this.header.after;
        Mapx.Entry<K, V> lastReturned = null;
        int expectedModCount = Mapx.this.modCount.get();

        AbstractIterator() {
        }

        public boolean hasNext() {
            return this.nextEntry != Mapx.this.header;
        }


        public void remove() {
            if (this.lastReturned == null) {
                throw new IllegalStateException();
            }
            Mapx.this.remove(this.lastReturned.key);
            this.lastReturned = null;
            this.expectedModCount = Mapx.this.modCount.get();
        }

        Mapx.Entry<K, V> nextEntry() {
            if (this.nextEntry == Mapx.this.header) {
                throw new NoSuchElementException();
            }
            if (Mapx.this.modCount.get() != this.expectedModCount) {
                while ((this.nextEntry != null) && (!Mapx.this.containsKey(this.nextEntry.key))) {
                    this.nextEntry = this.nextEntry.after;
                }
                this.expectedModCount = Mapx.this.modCount.get();
            }
            Mapx.Entry e = this.lastReturned = this.nextEntry;
            this.nextEntry = e.after;
            return e;
        }
    }

    static class Entry<K, V>
            implements Map.Entry<K, V> {
        Entry<K, V> before;
        Entry<K, V> after;
        final K key;
        V value;
        final int hash;
        Entry<K, V> next;
        long lastAccess = 0L;

        Entry(int h, K k, V v, Entry<K, V> n) {
            this.value = v;
            this.next = n;
            this.key = k;
            this.hash = h;
        }

        public K getKey() {
            return this.key == Mapx.NULL_KEY ? null : this.key;
        }

        public V getValue() {
            return this.value;
        }

        public V setValue(V newValue) {
            this.value = newValue;
            return this.value;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry e = (Entry) o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if ((k1 == k2) || ((k1 != null) && (k1.equals(k2)))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if ((v1 == v2) || ((v1 != null) && (v1.equals(v2)))) {
                    return true;
                }
            }
            return false;
        }

        public int hashCode() {
            return (this.key == Mapx.NULL_KEY ? 0 : this.key.hashCode()) ^ (this.value == null ? 0 : this.value.hashCode());
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }

        void recordRemoval(Mapx<K, V> m) {
            this.before.after = this.after;
            this.after.before = this.before;
        }

        void addBefore(Entry<K, V> existingEntry) {
            this.after = existingEntry;
            this.before = existingEntry.before;
            this.before.after = this;
            this.after.before = this;
        }
    }

    class EntryIterator extends Mapx<K, V>.AbstractIterator<Map.Entry<K, V>> {
        EntryIterator() {
            super();
        }

        public Mapx.Entry<K, V> next() {
            return nextEntry();
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        EntrySet() {
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            return Mapx.this.newEntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            Mapx.Entry candidate = Mapx.this.getEntry(e.getKey());
            return (candidate != null) && (candidate.equals(e));
        }

        public boolean remove(Object o) {
            return Mapx.this.removeMapping(o) != null;
        }

        public int size() {
            return Mapx.this.size;
        }

        public void clear() {
            Mapx.this.clear();
        }
    }

    class KeyIterator extends Mapx<K, V>.AbstractIterator<K> {
        KeyIterator() {
            super();
        }

        public K next() {
            return nextEntry().getKey();
        }
    }

    class KeySet extends AbstractSet<K> {
        KeySet() {
        }

        public Iterator<K> iterator() {
            return Mapx.this.newKeyIterator();
        }

        public int size() {
            return Mapx.this.size;
        }

        public boolean contains(Object o) {
            return Mapx.this.containsKey(o);
        }

        public boolean remove(Object o) {
            return Mapx.this.removeEntryForKey(o) != null;
        }

        public void clear() {
            Mapx.this.clear();
        }
    }

    static class ShamLock
            implements Lock {
        public void lock() {
        }

        public void lockInterruptibly()
                throws InterruptedException {
        }

        public boolean tryLock() {
            return true;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        public void unlock() {
        }

        public Condition newCondition() {
            return null;
        }
    }

    class ValueIterator extends Mapx<K, V>.AbstractIterator<V> {
        ValueIterator() {
            super();
        }

        public V next() {
            return nextEntry().value;
        }
    }

    class Values extends AbstractCollection<V> {
        Values() {
        }

        public Iterator<V> iterator() {
            return Mapx.this.newValueIterator();
        }

        public int size() {
            return Mapx.this.size;
        }

        public boolean contains(Object o) {
            return Mapx.this.containsValue(o);
        }

        public void clear() {
            Mapx.this.clear();
        }
    }
}

