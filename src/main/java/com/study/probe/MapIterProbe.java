package com.study.probe;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Day-07 第二段验证：HashMap 的 HashIterator 三个行为面。
 * <p>
 * 要能不看资料说出：
 * 1. entrySet 遍历中 map.remove() 为什么抛
 * 2. it.remove() 为什么不抛
 * 3. hasNext() 为什么不抛、next() 为什么抛（校验位置在哪）
 */
public class MapIterProbe {

    public static void main(String[] args) {
        scene1MapRemove();
        scene2ItRemove();
        scene3HasNextNoCheck();
        scene4RemoveLast();
    }

    private static Map<String, Integer> newMap() {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            m.put("k" + i, i);
        }
        return m;
    }

    /** 场景 1：遍历 entrySet 时调 map.remove() → 下一次 next() 抛。 */
    private static void scene1MapRemove() {
        Map<String, Integer> map = newMap();
        System.out.println("【场景 1】遍历 entrySet 时调 map.remove()");
        try {
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if ("k2".equals(e.getKey())) {
                    map.remove("k2");
                    System.out.println("    → map.remove(k2) 已执行完毕，此刻【没有】抛异常");
                }
            }
            System.out.println("  结果：未抛异常（说明判断错了）");
        } catch (ConcurrentModificationException ex) {
            System.out.println("  结果：抛 CME —— 抛点在下一轮 nextNode()，不在 map.remove");
        }
        System.out.println("  结论：removeNode 改了 modCount，迭代器快照停在构造时\n");
    }

    /** 场景 2：用 it.remove() → 不抛。 */
    private static void scene2ItRemove() {
        Map<String, Integer> map = newMap();
        System.out.println("【场景 2】用 Iterator.remove()");
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            if ("k2".equals(it.next().getKey())) {
                it.remove();
            }
        }
        System.out.println("  结果：size = " + map.size() + "，未抛异常");
        System.out.println("  结论：HashIterator.remove 删完把 expectedModCount 同步回 modCount\n");
    }

    /** 场景 3：hasNext() 不校验 —— 改了之后只调 hasNext 不抛，再调 next 才抛。 */
    private static void scene3HasNextNoCheck() {
        Map<String, Integer> map = newMap();
        System.out.println("【场景 3】hasNext() 与 next() 的校验位置差异");
        Iterator<String> it = map.keySet().iterator();
        it.next();                       // 先取一个，让迭代器进入正常状态
        map.remove("k4");                // 结构被改
        System.out.println("  改完之后调 hasNext()：" + it.hasNext() + "  ← 没抛");
        try {
            it.next();
            System.out.println("  再调 next()：未抛异常（说明判断错了）");
        } catch (ConcurrentModificationException ex) {
            System.out.println("  再调 next()：抛 CME");
        }
        System.out.println("  结论：校验只在 nextNode() 里，hasNext() 只读预取的 next 字段 —— 它不判断"
                + "「迭代器还有效吗」，只回答「预取节点非空」，所以这个 true 不是安全信号，是答非所问；\n"
                + "        这解释了 ArrayList 那个 [a,b,c] 删 b 不抛的现象为什么同构");
    }

    /** 场景 4：删掉的正好是迭代顺序里的最后一个 → 不抛。 */
    private static void scene4RemoveLast() {
        Map<String, Integer> map = newMap();
        List<String> keys = new ArrayList<>(map.keySet());   // 先摸清迭代顺序
        String lastKey = keys.get(keys.size() - 1);

        System.out.println("【场景 4】遍历中删掉迭代顺序里的最后一个 key（" + lastKey + "）");
        try {
            for (String k : map.keySet()) {
                if (k.equals(lastKey)) {
                    map.remove(k);
                }
            }
            System.out.println("  结果：未抛异常，size = " + map.size());
        } catch (ConcurrentModificationException ex) {
            System.out.println("  结果：抛 CME（说明判断错了）");
        }
        System.out.println("  结论：删完 next 已是 null，hasNext() 直接为 false，循环正常结束 —— "
                + "一次 next() 都没再调，所以没机会校验");
    }
}
