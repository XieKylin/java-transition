package com.study.map;


import java.io.Serializable;
import java.util.*;

public class MyHashMap<K, V> extends AbstractMap<K, V>
        implements Map<K, V>, Cloneable, Serializable {
    transient MyHashMap.Node<K, V>[] table;

    transient int size;

    int threshold;

    public MyHashMap() {
        table = (Node<K, V>[]) new Node[16];
        threshold = 12;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            return o instanceof Map.Entry<?, ?> e
                    && Objects.equals(key, e.getKey())
                    && Objects.equals(value, e.getValue());
        }
    }

//    public V put(K key, V value) {
//
//    }

    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    static int indexFor(int h, int n) {
        return (n - 1) & h;
    }

    public V get(Object key) {
        int h = hash(key);
        int i = indexFor(h, table.length);
        Node<K, V> e = table[i];
        while (e != null) {
            if (h == e.hash && (key == e.getKey() || (key != null && key.equals(e.getKey())))) {
                return e.getValue();
            }
            e = e.next;
        }
        return null;
    }

    public V put(K key, V value) {
        int h = hash(key);
        int index = indexFor(h, table.length);
        Node<K, V> first = table[index];
        Node<K, V> e;
        if (first == null) {
            e = new Node<>(h, key, value, null);
            table[index] = e;
            size++;
            return null;
        }
        if (h == first.hash
                && (first.getKey() == key || (key != null && key.equals(first.getKey())))) {
            V oldValue = first.getValue();
            first.setValue(value);
            return oldValue;
        }
        while (true) {
            if (first.next == null) {
                e = new Node<>(h, key, value, null);
                first.next = e;
                size++;
                return null;
            }
            first = first.next;
            if (h == first.hash
                    && (first.getKey() == key || (key != null && key.equals(first.getKey())))) {
                V oldValue = first.getValue();
                first.setValue(value);
                return oldValue;
            }
        }
    }


}
