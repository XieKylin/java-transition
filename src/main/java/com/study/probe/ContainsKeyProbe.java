package com.study.probe;

import com.study.map.MyHashMap;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 探针：`containsKey` 覆写前后，各走了多少次 `equals`。
 *
 * 为什么要数 equals 而不是看返回值对错 —— 10-30 的教训：
 *   两个实现返回的都是 false，结果一样，差别只在"走了多少路"。
 *   一个统计量若能由多个原因产生，就必须单独归因；这里 equals 次数只能由「遍历范围」产生。
 *
 * 运行：java -cp target/classes com.study.probe.ContainsKeyProbe
 */
public class ContainsKeyProbe {

    /** 每次 equals 被调用时 +1。用反射/肉眼都靠不住，让程序自己数 */
    static final AtomicInteger EQUALS = new AtomicInteger();

    static final class K {
        final int v;

        K(int v) {
            this.v = v;
        }

        @Override
        public int hashCode() {
            return v;
        }

        @Override
        public boolean equals(Object o) {
            EQUALS.incrementAndGet();
            return o instanceof K && ((K) o).v == v;
        }

        @Override
        public String toString() {
            return "K" + v;
        }
    }

    /** AbstractMap.containsKey 的原始实现（照抄 JDK 源码，用于对照） */
    private static boolean defaultContainsKey(Map<?, ?> m, Object key) {
        Iterator<? extends Map.Entry<?, ?>> i = m.entrySet().iterator();
        if (key == null) {
            while (i.hasNext()) {
                if (i.next().getKey() == null) return true;
            }
        } else {
            while (i.hasNext()) {
                if (key.equals(i.next().getKey())) return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        MyHashMap<K, String> map = new MyHashMap<>();
        int n = 1000;
        for (int i = 0; i < n; i++) {
            map.put(new K(i), "v" + i);
        }
        System.out.println("装载 " + n + " 个条目，size=" + map.size());

        // 查一个不存在的 key：两种实现都得走完自己的搜索范围
        K missing = new K(999_999);
        EQUALS.set(0);
        boolean a = defaultContainsKey(map, missing);
        int equalDefault = EQUALS.get();

        EQUALS.set(0);
        boolean b = map.containsKey(missing);
        int equalOurs = EQUALS.get();

        System.out.println();
        System.out.println("[缺失 key]  默认实现 containsKey = " + a + "，equals 调用 " + equalDefault + " 次（= 全表条目数）");
        System.out.println("[缺失 key]  覆写后 containsKey   = " + b + "，equals 调用 " + equalOurs + " 次（= 该桶链长）");
        System.out.println("[缺失 key]  两者结论一致 = " + (a == b));

        // 查一个存在的 key
        K hit = new K(500);
        EQUALS.set(0);
        boolean c = defaultContainsKey(map, hit);
        int equalDefaultHit = EQUALS.get();
        EQUALS.set(0);
        boolean d = map.containsKey(hit);
        int equalOursHit = EQUALS.get();
        System.out.println();
        System.out.println("[命中 key]  默认实现 = " + c + "，equals " + equalDefaultHit + " 次");
        System.out.println("[命中 key]  覆写后   = " + d + "，equals " + equalOursHit + " 次");
        System.out.println("[命中 key]  两者结论一致 = " + (c == d));

        // null key
        map.put(null, "nullVal");
        System.out.println();
        System.out.println("[null key] containsKey(null)=" + map.containsKey(null)
                + "  get(null)=" + map.get(null));

        // 结论判定：让程序自证，不靠人看数字
        boolean ok = (a == b) && (c == d) && equalOurs < equalDefault && map.containsKey(null);
        System.out.println();
        System.out.println("== 判定：" + (ok ? "覆写后仍正确，且 equals 调用量下降" : "不成立，需检查") + " ==");
        if (!ok) {
            System.exit(1);
        }
    }
}
