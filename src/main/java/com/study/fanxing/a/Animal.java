package com.study.fanxing.a;

class Animal {
    public void speak()        { System.out.println("animal"); }
    public final void fspeak() { System.out.println("final"); }
    public static void util()  { System.out.println("static"); }
    private void secret()      { System.out.println("secret"); }
    public void callSecret()   { secret(); }          // ← private 的调用藏在方法体里
}




