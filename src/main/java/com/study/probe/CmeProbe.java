package com.study.probe;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/**
 * Day-07 输入验证片段：ConcurrentModificationException 的四个触发面。
 * <p>
 * 只验证结论，不服务任何功能实现。跑完要能不看资料说出：
 * 1. for-each 里 remove 为什么抛
 * 2. iterator.remove() 为什么不抛
 * 3. 下标 for 为什么不抛但会漏元素
 * 4. set() 为什么不抛
 */
public class CmeProbe {

    public static void main(String[] args) {
        scene1ForEachRemove();
        scene2IteratorRemove();
        scene3IndexFor();
        scene4SetNoCme();
    }

    /** 场景 1：for-each 里调 list.remove → 下一次 next() 抛 CME。 */
    private static void scene1ForEachRemove() {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        System.out.println("【场景 1】for-each 里调 list.remove(Object)");
        try {
            for (Integer x : list) {
                if (x == 3) {
                    list.remove(x);   // x 是 Integer → 走 remove(Object)，不是 remove(int)
                    System.out.println("    → list.remove(3) 已执行完毕，此刻【没有】抛异常");
                }
            }
            System.out.println("  结果：未抛异常（说明预期判断错了）");
        } catch (ConcurrentModificationException e) {
            System.out.println("  结果：抛 CME —— 抛点在下一轮 for-each 的 next()，不在 remove");
            System.out.println("    栈顶：" + e.getStackTrace()[0]);
        }
        System.out.println("  结论：for-each 编译后就是 Iterator 循环，remove 改了 modCount，"
                + "下一次 next() 比对 expectedModCount 就炸\n");
    }

    /** 场景 2：用 Iterator.remove() → 不抛。 */
    private static void scene2IteratorRemove() {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        System.out.println("【场景 2】用 Iterator.remove()");
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            Integer x = it.next();
            if (x == 3) {
                it.remove();
            }
        }
        System.out.println("  结果：list = " + list + "，未抛异常");
        System.out.println("  结论：Itr.remove 删完会把 expectedModCount 同步成最新的 modCount，"
                + "两边继续相等\n");
    }

    /** 场景 3：下标 for 循环里 remove → 不抛，但会漏元素。 */
    private static void scene3IndexFor() {
        List<Integer> list = new ArrayList<>(List.of(2, 4, 6, 8));
        System.out.println("【场景 3】下标 for 循环里 remove(i)（想删掉所有偶数）");
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) % 2 == 0) {
                list.remove(i);
            }
        }
        System.out.println("  结果：list = " + list + "（预期 []），未抛异常");
        System.out.println("  结论：不碰迭代器 → 没有 CME 检查；但 remove 后元素整体左移，"
                + "i++ 正好跳过后一个元素\n");
    }

    /** 场景 4：遍历中 set() 改值 → 不抛。 */
    private static void scene4SetNoCme() {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        System.out.println("【场景 4】遍历中 set(0, 99) 改值");
        try {
            for (Integer x : list) {
                list.set(0, 99);
            }
            System.out.println("  结果：未抛异常，list = " + list);
        } catch (ConcurrentModificationException e) {
            System.out.println("  结果：抛 CME（说明预期判断错了）");
        }
        System.out.println("  结论：set 不是结构性修改，modCount 不变 —— modCount 数的是「元素个数/结构变了」"
                + "的次数，不是「内容变过」");
    }
}
