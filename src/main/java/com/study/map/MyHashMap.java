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
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    private final Node<K, V>[] t = MyHashMap.this.table;
                    private int nextIndex = 0;        // 下一个待检查的桶
                    private Node<K, V> nextNode = null;   // 下一个待返回的节点

                    @Override
                    public boolean hasNext() {
                        while (nextNode == null && nextIndex < t.length) {
                            nextNode = t[nextIndex++];
                        }
                        return nextNode != null;
                    }

                    @Override
                    public Entry<K, V> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        Node<K, V> e = nextNode;
                        nextNode = e.next;
                        if (nextNode == null) {
                            hasNext();
                        }
                        return e;
                    }
                };
            }

            @Override
            public int size() {
                return MyHashMap.this.size;
            }
        };
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
            if (++size > threshold) {
                resizeBrute();
            }
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
                if (++size > threshold) {
                    resizeBrute();
                }
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

    @SuppressWarnings("unchecked")
    private void resizeBrute() {
        Node<K, V>[] oldTable = table;
        int oldCap = oldTable.length;
        int newCap = oldCap << 1;

        Node<K, V>[] newTable = new Node[newCap];

        for (Node<K, V> kvNode : oldTable) {
            Node<K, V> e = kvNode;
            while (e != null) {
                Node<K, V> next = e.next;
                int index = indexFor(e.hash, newCap);
                Node<K, V> last;
                if ((last = newTable[index]) == null) {
                    newTable[index] = e;
                    e.next = null;
                    e = next;
                    continue;
                }
                while (last.next != null) {
                    last = last.next;
                }
                last.next = e;
                e.next = null;
                e = next;
            }
        }

        table = newTable;
        threshold = newCap * 3 / 4;
    }

    @Override
    public V remove(Object k) {
        int h = hash(k);
        int index = indexFor(h, table.length);
        Node<K, V> prev = null;
        Node<K, V> e = table[index];
        while (e != null) {
            if (h == e.hash && (e.getKey() == k || (k != null && k.equals(e.getKey())))) {
                if (prev == null) {
                    table[index] = e.next;
                } else {
                    prev.next = e.next;
                }
                size--;
                return e.getValue();
            }
            prev = e;
            e = e.next;
        }
        return null;
    }

}
