package com.study.probe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 探针：检验 Day-13 关于 String 的三条结论，全部让程序自证（断言 + 计数，不靠肉眼看日志）。
 *
 * 结论：
 *   ① 折叠判据不是「是不是字面量」，而是「编译期能否算出唯一值」——
 *      static final / 局部 final 且初始化表达式是常量 → 折叠（常量变量）；
 *      final String x = new String("a") → 不折叠（final 只锁引用，值不是常量）。
 *   ② intern：new String("x").intern() == "x" 为 true；运行期拼出来的串同样能收回池里同一个对象。
 *   ③ new String("hello") 是两个对象共用一份 byte[]（构造器 this.value = original.value 零拷贝）；
 *      length() = value.length >> coder（ASCII coder=0 不移，中文 coder=1 移一位）。
 *
 * 反射部分需要开包：
 *   java --add-opens java.base/java.lang=ALL-UNNAMED -cp target/classes com.study.probe.StringInternProbe
 * 不开也能跑，第 ③ 组会打印「跳过」并说明原因。
 */
public class StringInternProbe {

    /** 常量变量：static final + 常量初始化表达式 → 编译期折叠 */
    static final String PREFIX = "a";
    /** 非 final 的 static：运行期才读，不折叠 */
    static String prefix2 = "a";

    private static int pass;
    private static int fail;

    public static void main(String[] args) throws Exception {
        System.out.println("JVM: " + System.getProperty("java.version")
                + "  (" + System.getProperty("java.vendor") + ")");
        System.out.println();

        folding();
        intern();
        sharedBackingArray();
        summary();
    }

    // ---------------- ① 编译期折叠 ----------------

    private static void folding() {
        System.out.println("== ① 折叠判据：编译期能否算出唯一值 ==");

        String lit = "a" + "b";
        check("字面量拼接  \"a\"+\"b\" == \"ab\"", lit == "ab", true);

        String viaConst = PREFIX + "b";
        check("static final PREFIX+\"b\" == \"ab\"", viaConst == "ab", true);

        final String localFinal = "a";
        String viaLocalFinal = localFinal + "b";
        check("局部 final L=\"a\"  L+\"b\" == \"ab\"", viaLocalFinal == "ab", true);

        String viaNonFinal = prefix2 + "b";
        check("static 非 final  +\"b\" == \"ab\"", viaNonFinal == "ab", false);

        final String boxed = new String("a");
        String viaNew = boxed + "b";
        check("final String x=new String(\"a\")  x+\"b\" == \"ab\"", viaNew == "ab", false);

        System.out.println();
    }

    // ---------------- ② intern ----------------

    private static void intern() {
        System.out.println("== ② intern：池里登记的是哪个对象 ==");

        String lit = "hello";
        String s1 = new String("hello");
        check("new String(\"hello\") == \"hello\"", s1 == lit, false);
        check("new String(\"hello\").intern() == \"hello\"", s1.intern() == lit, true);
        check("两者不是同一个对象（identityHashCode 不同）",
                System.identityHashCode(s1) != System.identityHashCode(lit), true);

        // 运行期才拼出来的串（JDK 9+ 走 invokedynamic），编译期算不出，故不在池里
        String s2 = new String("hel") + "lo";
        check("new String(\"hel\")+\"lo\" == \"hello\"", s2 == lit, false);
        check("它的 intern() == \"hello\"", s2.intern() == lit, true);
        check("两次 intern 拿到同一个对象", s1.intern() == s2.intern(), true);

        System.out.println();
    }

    // ---------------- ③ 共享底层数组 + coder ----------------

    private static void sharedBackingArray() throws Exception {
        System.out.println("== ③ 两个对象一份 byte[]，length() = value.length >> coder ==");

        try {
            Field valueField = String.class.getDeclaredField("value");
            valueField.setAccessible(true);
            Method coder = String.class.getDeclaredMethod("coder");
            coder.setAccessible(true);

            String lit = "hello";
            String copy = new String(lit);
            byte[] litBytes = (byte[]) valueField.get(lit);
            byte[] copyBytes = (byte[]) valueField.get(copy);

            check("new String(lit) 的 value 与 lit 的 value 是同一个数组",
                    litBytes == copyBytes, true);
            check("两个 String 对象不是同一个（identityHashCode 不同）",
                    System.identityHashCode(lit) != System.identityHashCode(copy), true);

            System.out.println("    ASCII \"hello\"：value.length=" + litBytes.length
                    + "  coder=" + coder.invoke(lit) + "  length()=" + lit.length());

            String cn = "中文";
            byte[] cnBytes = (byte[]) valueField.get(cn);
            System.out.println("    中文  \"中文\" ：value.length=" + cnBytes.length
                    + "  coder=" + coder.invoke(cn) + "  length()=" + cn.length());
            check("中文 4 字节 / coder=1 / length()=2（4>>1=2）",
                    cnBytes.length == 4 && (int) coder.invoke(cn) == 1 && cn.length() == 2, true);

        } catch (NoSuchFieldException | NoSuchMethodException e) {
            System.out.println("    跳过：当前 JDK 里没有 value/coder 字段（" + e.getMessage() + "）");
        } catch (RuntimeException | IllegalAccessException e) {
            System.out.println("    跳过：反射被模块系统挡住，加 --add-opens java.base/java.lang=ALL-UNNAMED 再跑");
            System.out.println("    原因：" + e.getClass().getSimpleName());
        }
        System.out.println();
    }

    // ---------------- 工具 ----------------

    private static void check(String label, boolean actual, boolean expected) {
        boolean ok = actual == expected;
        if (ok) {
            pass++;
        } else {
            fail++;
        }
        System.out.println("    [" + (ok ? "PASS" : "FAIL") + "] " + label
                + "  → 实测 " + actual + "，预期 " + expected);
    }

    private static void summary() {
        System.out.println("== 汇总 ==");
        System.out.println("    PASS " + pass + " / FAIL " + fail
                + (fail == 0 ? "  → 三条结论全部成立" : "  → 有断言不成立，结论需修正"));
        if (fail != 0) {
            System.exit(1);
        }
    }
}
