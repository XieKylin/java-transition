package com.study.probe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 探针：检验「迭代器是不是 map 的数据快照」。
 *
 * 结论（2026-11-03 实测）：不是。迭代器 = 活视图 + 一个计数快照。
 *   C 场景：覆盖写不记账 -> 迭代器不抛，但它读到的是 new-b 而不是创建时的 old-b
 *
 * 反证思路：如果迭代器真是数据快照（CopyOnWriteArrayList 那种写时复制），
 * 读写互不干扰、遍历永远安全，根本不需要 CME。CME 存在的前提就是共享同一份链表。
 *
 * 附带的失败尝试（保留，用于说明「扩容不记账」为何观察不到）：
 *   曾想用「连续 put 撑扩容、看旧迭代器抛不抛」证明扩容不记账，
 *   结果必然抛 —— 因为那 40 次插入本身就要记账。触发扩容的动作必然伴随记账，
 *   所以「扩容不记账」在单线程里不是一个可独立观测的行为，只能读源码确认。
 *
 * 运行：mvn -q compile 后
 *      java -Dstdout.encoding=UTF-8 -cp target/classes com.study.probe.SnapshotProbe
 */
public class SnapshotProbe {

    public static void main(String[] args) {
        HashMap<String, String> m = new HashMap<>();
        m.put("a", "old-a");
        m.put("b", "old-b");

        String seenB = null;
        try {
            for (Map.Entry<String, String> e : m.entrySet()) {
                if ("a".equals(e.getKey())) {
                    m.put("b", "new-b");    // 覆盖写：不记账，迭代器不会抛
                }
                if ("b".equals(e.getKey())) {
                    seenB = e.getValue();   // 迭代器走到 b 时读到的值
                }
            }
        } catch (RuntimeException e) {
            System.out.println("意外抛了 " + e.getClass().getSimpleName());
            return;
        }

        System.out.println("创建迭代器时 b 的值 = old-b");
        System.out.println("迭代过程中 b 的值   = " + seenB);
        System.out.println("读到的是创建时的旧值 = " + "old-b".equals(seenB)
                + "   （false 说明迭代器跟的是活节点，不是快照）");
    }
}
