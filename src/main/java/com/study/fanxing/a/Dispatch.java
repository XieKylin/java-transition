package com.study.fanxing.a;

class Dispatch {
    public static void main(String[] args) {
        Animal a = new Dog();      // 声明类型 Animal，实际类型 Dog
        a.speak();
        a.fspeak();
        Animal.util();
        a.callSecret();
    }
}