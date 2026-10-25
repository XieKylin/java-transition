package com.study.fanxing.a;

class Dog extends Animal {
    @Override public void speak() {
        super.speak();                                 // ← super 调用
        System.out.println("dog");
    }
}