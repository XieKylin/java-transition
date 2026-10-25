package com.study;

public class MyArrayListDemo {
    public static void main(String[] args) {
        MyArrayList<Hello> hellos = new MyArrayList<>();
        for (int i = 0; i < 100000; i++) {
            hellos.add(new Hello());
            System.out.println("index: " + i + ", capacity: " + hellos.capacity());
        }
    }
}
