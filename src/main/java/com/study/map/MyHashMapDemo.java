package com.study.map;
public class MyHashMapDemo {
    public static void main(String[] args) {
        String[] keys = {"a", "b", "c", "d", "e", "f", "g", "h"};
        int hash;
        for (String key : keys) {
            System.out.println("hash: " + (hash = MyHashMap.hash(key)) + ", index: " + MyHashMap.indexFor(hash, 16));
        }

        Integer[] intKeys = {65536, 131072, 196608, 262144, 327680, 393216, 458752, 524288};
        for (Integer intKey : intKeys) {
            System.out.println("hash: " + (hash = MyHashMap.hash(intKey)) + ", index: " + MyHashMap.indexFor(hash, 16));

        }
    }
}
