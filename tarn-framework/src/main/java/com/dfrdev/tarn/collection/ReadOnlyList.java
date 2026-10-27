package com.dfrdev.tarn.collection;


import com.dfrdev.tarn.core.TarnException;

import java.util.*;

public class ReadOnlyList<E> extends ArrayList<E> {
    private static final long serialVersionUID = 1L;
    List<? extends E> data;

    public ReadOnlyList(List<? extends E> data) {
        if (data == null) {
            throw new NullPointerException();
        }
        this.data = data;
    }

    public ReadOnlyList(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        ArrayList list = new ArrayList(c.size());
        for (Object e : c) {
            list.add(e);
        }
        this.data = list;
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

    public boolean add(E o) {
        throw new ListReadOnlyException();
    }

    public boolean remove(Object o) {
        throw new ListReadOnlyException();
    }

    public boolean containsAll(Collection<?> c) {
        return this.data.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        throw new ListReadOnlyException();
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        throw new ListReadOnlyException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new ListReadOnlyException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new ListReadOnlyException();
    }

    public void clear() {
        throw new ListReadOnlyException();
    }

    public E get(int index) {
        return this.data.get(index);
    }

    public E set(int index, E element) {
        throw new ListReadOnlyException();
    }

    public void add(int index, E element) {
        throw new ListReadOnlyException();
    }

    public E remove(int index) {
        throw new ListReadOnlyException();
    }

    public int indexOf(Object o) {
        return this.data.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return this.data.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    public ListIterator<E> listIterator(int index) {
        return new ListItr(index);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        throw new ListReadOnlyException("ReadOnlyList's subList is not supported!");
    }

    private class Itr
            implements Iterator<E> {
        int cursor = 0;

        private Itr() {
        }

        public boolean hasNext() {
            return this.cursor != ReadOnlyList.this.size();
        }

        public E next() {
            try {
                return ReadOnlyList.this.get(this.cursor++);
            } catch (IndexOutOfBoundsException e) {
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new ReadOnlyList.ListReadOnlyException();
        }
    }

    private class ListItr extends ReadOnlyList<E>.Itr
            implements ListIterator<E> {
        ListItr(int index) {

            this.cursor = index;
        }

        public boolean hasPrevious() {
            return this.cursor != 0;
        }

        public E previous() {
            try {
                int i = this.cursor - 1;
                return ReadOnlyList.this.get(i);
            } catch (IndexOutOfBoundsException e) {
            }
            throw new NoSuchElementException();
        }

        public int nextIndex() {
            return this.cursor;
        }

        public int previousIndex() {
            return this.cursor - 1;
        }

        public void set(E o) {
            throw new ReadOnlyList.ListReadOnlyException();
        }

        public void add(E o) {
            throw new ReadOnlyList.ListReadOnlyException();
        }
    }

    public static class ListReadOnlyException extends TarnException {
        private static final long serialVersionUID = 1L;

        public ListReadOnlyException() {
            super("");
        }

        public ListReadOnlyException(String message) {
            super("");
        }
    }
}

