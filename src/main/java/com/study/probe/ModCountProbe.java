package com.study.probe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探针：检验「modCount 的记账判据是不是条目数量」。
 *
 * 结论（2026-11-03 实测）：不是数量，是「条目集合 + 迭代顺序」。
 *   A 场景：remove 再 put 回同一个 key —— size 前后相等，照样抛 CME（旧节点被换成新节点）
 *   B 场景：LinkedHashMap accessOrder=true 只调一次 get —— size 与 key 集合全不变，照样抛 CME（顺序变了）
 *
 * 运行：mvn -q compile 后
 *      java -Dstdout.encoding=UTF-8 -cp target/classes com.study.probe.ModCountProbe
 */
public class ModCountProbe {

    public static void main(String[] args) {
        scenarioA();
        scenarioB();
    }

    /** A：先删后加回同一个 key —— 数量回到原值 */
    private static void scenarioA() {
        HashMap<Integer, String> m = new HashMap<>();
        m.put(1, "a");
        m.put(2, "b");
        m.put(3, "c");
        int before = m.size();

        Iterator<Map.Entry<Integer, String>> it = m.entrySet().iterator();
        it.next();                      // 先消耗一个，让迭代器处于「中途」

        m.remove(2);                    // 摘掉旧节点
        m.put(2, "b");                  // 放回一个新建的节点，key/value 都一样
        int after = m.size();

        System.out.println("[A] 数量：before=" + before + "  after=" + after
                + "  数量相等=" + (before == after));
        System.out.println("[A] 但 key=2 的旧节点已摘下，现在是新建的节点：条目集合变了");
        report(it);
    }

    /** B：accessOrder 下只 get 一次 —— 数量、集合都没变，只改了顺序 */
    private static void scenarioB() {
        LinkedHashMap<Integer, String> lru = new LinkedHashMap<>(16, .75f, true);
        lru.put(1, "a");
        lru.put(2, "b");
        lru.put(3, "c");
        int before = lru.size();

        Iterator<Integer> it = lru.keySet().iterator();
        it.next();

        lru.get(1);                     // 只是读一次，把 key=1 挪到队尾（afterNodeAccess 记账）
        int after = lru.size();

        System.out.println("[B] 数量：before=" + before + "  after=" + after
                + "  数量相等=" + (before == after));
        System.out.println("[B] 三个 key 一个没少、一个没多：条目集合没变，只有迭代顺序变了");
        report(it);
    }

    private static void report(Iterator<?> it) {
        try {
            while (it.hasNext()) {
                it.next();
            }
            System.out.println("    -> 遍历正常结束，没有抛异常");
        } catch (RuntimeException e) {
            System.out.println("    -> 抛了 " + e.getClass().getSimpleName());
        }
    }
}
