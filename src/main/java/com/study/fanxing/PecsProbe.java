package com.study.fanxing;

import java.util.ArrayList;
import java.util.List;

/**
 * Day-21（2026-11-13）泛型 ① PECS 实验。
 *
 * <p>A 段：drain(List&lt;? extends Number&gt;, List&lt;? super Number&gt;) —— 签名本身就是 PECS 的答案。
 * <p>B 段：? extends 一侧唯一能写进去的值是 null。
 * <p>E 段：数组是协变的，所以错误被推到运行期；泛型是不变的，错误被拦在编译期。
 *
 * <p>故意编不过的对照实验在 playground/PecsFail.java，报错原文在 playground/pecs-error.txt。
 */
public final class PecsProbe {

    private PecsProbe() {
    }

    /**
     * A 段：src 是生产者（只读，元素类型至少是 Number），dst 是消费者（只写，能装下 Number）。
     * 方法体只有一句 dst.add(n)，能写成这样全靠签名里那两个通配符。
     */
    static void drain(List<? extends Number> src, List<? super Number> dst) {
        for (Number n : src) {
            dst.add(n);
        }
    }

    /**
     * B 段：? extends Number 的 add 参数类型是 CAP#1（"某个 Number 的子类，但不知道是哪个"）。
     * 具体值（如 1 → Integer）不保证是 CAP#1，故编译失败；
     * null 是所有引用类型共同的值，故是唯一放行的写法。
     */
    static void addNullProbe(List<? extends Number> src) {
        int before = src.size();
        src.add(null);
        // src.add(1);   // 去掉注释即编译失败，报错原文见 playground/pecs-error.txt 的 B 反例
        System.out.println("B: add(null) 前 size = " + before + "，后 size = " + src.size());
        System.out.println("B: 末元素 = " + src.get(src.size() - 1));
    }

    /**
     * E 段：String[] 是 Object[] 的子类型（数组协变），编译期不做元素类型检查，
     * 检查推到运行期的 aastore 指令上 → ArrayStoreException。
     */
    static void arrayCovarianceProbe() {
        Object[] arr = new String[1];
        try {
            arr[0] = 1;   // 自动装箱成 Integer，往"运行时是 String[]"的数组里塞
            System.out.println("E: 没抛异常 —— 说明前面的推理错了");
        } catch (ArrayStoreException e) {
            System.out.println("E: 抛出 " + e.getClass().getName());
            System.out.println("E: getMessage() = " + e.getMessage());
        }
        // 同一个错误若用泛型写，编译期就过不去：
        // List<Object> list = new ArrayList<String>();
        // 见 playground/PecsFail.java 的 F 段
    }

    public static void main(String[] args) {
        List<? extends Number> src = new ArrayList<>(List.of(1, 2, 3));
        List<? super Number> dst = new ArrayList<>();

        drain(src, dst);
        System.out.println("A: dst = " + dst + "，size = " + dst.size());

        addNullProbe(src);
        arrayCovarianceProbe();
    }
}
