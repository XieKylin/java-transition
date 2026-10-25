package com.study.fanxing.b;

class Probe {
    public static void main(String[] args) {
        for (var m : StringBox.class.getDeclaredMethods()) {
            System.out.println(
                m.getReturnType().getSimpleName() + " " + m.getName()
                + "  bridge=" + m.isBridge()
                + "  synthetic=" + m.isSynthetic());
        }
    }
}