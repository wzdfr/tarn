package com.dfrdev.tarn.collection;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Queuex<T>
        implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private Object[] elementData;
    private int max;
    private int pos;
    private int size;
    private ExitEventListener<Object, T> listener;
    private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

    private Lock rlock = this.rwlock.readLock();

    private Lock wlock = this.rwlock.writeLock();

    public Queuex(int max) {
        this.max = max;
        this.elementData = new Object[max];
    }

    public T get(int index) {
        if (this.size <= index) {
            throw new RuntimeException("Index is out of range：" + index);
        }
        this.rlock.lock();
        try {
            Object t = this.elementData[((this.pos + index) % this.max)];
            return (T) t;
        } finally {
            this.rlock.unlock();
        }
    }

    public T push(T o) {
        this.wlock.lock();
        try {
            if (this.size == this.max) {
                Object r = this.elementData[this.pos];
                this.elementData[this.pos] = o;
                this.pos = ((this.pos + 1) % this.max);
                if (this.listener != null) {
                    this.listener.onExit(null, (T) r);
                }
                return (T) r;
            }
            this.elementData[((this.pos + this.size) % this.max)] = o;
            this.size += 1;
            return null;
        } finally {
            this.wlock.unlock();
        }
    }

    public boolean contains(Object v) {
        if (v == null) {
            return false;
        }
        this.rlock.lock();
        try {
            for (Object element : this.elementData) {
                if (v.equals(element)) {
                    return true;
                }
            }
            return false;
        } finally {
            this.rlock.unlock();
        }
    }

    public boolean remove(Object v) {
        if (v == null) {
            return false;
        }
        this.rlock.lock();
        try {
            for (int i = 0; i < this.size; i++) {
                if (v.equals(get(i))) {
                    remove(i);
                }
            }
            return true;
        } finally {
            this.rlock.unlock();
        }
    }

    public T remove(int index) {
        if (this.size <= index) {
            throw new RuntimeException("Index is out of range：" + index);
        }
        this.wlock.lock();
        try {
            Object r = get(index);
            index = (index + this.pos) % this.max;
            Object[] newarr = new Object[this.max];
            if (this.pos == 0) {
                System.arraycopy(this.elementData, 0, newarr, 0, index);
                System.arraycopy(this.elementData, index + 1, newarr, index, this.max - index - 1);
            } else {
                if (index >= this.pos) {
                    System.arraycopy(this.elementData, this.pos, newarr, 0, index - this.pos);
                    System.arraycopy(this.elementData, index + 1, newarr, index - this.pos, this.max - index - 1);
                    System.arraycopy(this.elementData, 0, newarr, this.max - this.pos - 1, this.pos);
                } else {
                    System.arraycopy(this.elementData, this.pos, newarr, 0, this.max - this.pos);
                    System.arraycopy(this.elementData, 0, newarr, this.max - this.pos, index);
                    System.arraycopy(this.elementData, index + 1, newarr, this.max - this.pos + index, this.pos - index);
                }
                this.pos = 0;
            }
            this.elementData = newarr;
            this.size -= 1;
            return (T) r;
        } finally {
            this.wlock.unlock();
        }
    }

    public void clear() {
        this.wlock.lock();
        try {
            this.elementData = new Object[this.max];
            this.size = 0;
        } finally {
            this.wlock.unlock();
        }
    }

    public int size() {
        return this.size;
    }

    public void setExitEventListener(ExitEventListener<Object, T> listener) {
        this.listener = listener;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; (i < 20) && (i < this.size); i++) {
            if (i != 0) {
                sb.append(" , ");
            }
            sb.append(get(i));
        }
        sb.append("}");
        return sb.toString();
    }

    public int getMax() {
        return this.max;
    }
}

