package com.study;

import java.util.*;

public class MyArrayList<E> implements Iterable<E> {
    private Object[] elementData;

    private int size;

    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    private class Itr implements Iterator<E> {
        int cursor;

        public boolean hasNext() {
            return cursor != size;
        }

        public E next() {
            if (cursor >= size) {
                throw new NoSuchElementException();
            }
            return (E) elementData[cursor++];
        }
    }

    public MyArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    public boolean add(E e) {
        if (size == elementData.length)
            elementData = grow();
        elementData[size++] = e;
        return true;
    }

    public void add(int index, E element) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        final int s;
        Object[] elementData;
        if ((s = size) == (elementData = this.elementData).length) {
            elementData = grow();
        }
        System.arraycopy(elementData, index, elementData, index + 1, s - index);
        elementData[index] = element;
        size = s + 1;
    }

    public E get(int index) {
        checkIndex(index);
        return (E) elementData[index];
    }

    private void checkIndex(int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private Object[] grow() {
        int oldCapacity = elementData.length;
        int newCapacity;
        if (oldCapacity == 0) {
            newCapacity = 10;
        } else {
            newCapacity = oldCapacity + (oldCapacity >> 1);
        }
        return elementData = Arrays.copyOf(elementData, newCapacity);
    }

    public int capacity() {
        return elementData.length;
    }

    public int size() {
        return size;
    }

}
