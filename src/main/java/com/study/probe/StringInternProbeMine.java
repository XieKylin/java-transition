package com.study.probe;

import com.study.probe.ObjectContractProbe.B;

public class StringInternProbeMine {

    public final static String PREFIX = "a";

    public static String PREFIX2 = "a";

    public static void main(String[] args) {
        check1();
        check2();
        check3();
        check4();
        check5();
    }

    public static void check1() {
        String A = "a" + "b"; // 编译期折叠，池里有 "ab"
        String B = "ab";      // 池里有 "ab"
        check("check1", A == B,true); // true
    }

    public static void check2() {
        String A = "ab";              
        String B = PREFIX + "b";   
        System.out.println(A == B);
    }

    public static void check3() {
        String A = "ab";              
        String B = PREFIX2 + "b";   
        check("check3", A == B,true);
    }

    public static void check4() {
        String A = "hello";              // ① 字面量，池里现在有 "hello"
        String B = new String("hello");   // ② 运行期拼出的新对象
        check("check4", A == B.intern(),true);
        // System.out.println(b == b.intern());
        // System.out.println(a == b.intern());
        // System.out.println(System.identityHashCode(a));
        // System.out.println(System.identityHashCode(b));
        // System.out.println(System.identityHashCode(b.intern()));
    }

    public static void check5() {
        long timeMillis = System.currentTimeMillis();
        String a = "hello" + timeMillis;
        String b = a.intern();
        check("check5", a == b.intern(),true);
    }

    static void check(String label, boolean actual, boolean expected) {
        System.out.println((actual == expected ? "[PASS] " : "[FAIL] ") + label);
    }


}
