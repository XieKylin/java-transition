package com.study.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyHashMap 回归测试。
 *
 * 来源：MyHashMapDemo 的第 1~5 段验收。迁移时把「手写 PASS/FAIL 字符串」换成了真正的断言。
 *
 * 三条用例设计硬规则（来自 问题卡 02 / 03）：
 *   1. 查找必须用「新对象」，逼它走 equals，不能复用 put 时的引用
 *   2. 冲突链的 key 必须 > 127，避开 Integer 缓存
 *   3. 每条 if 分支都要有能单独触发它的用例
 */
class MyHashMapTest {

    /** 冲突链专用：三个 key 在 cap=16 时同落桶 8，且都 > 127（不在 Integer 缓存里） */
    private static final int[] SAME_BUCKET = {1000, 1016, 1032};

    /** 8/24/40/56 在 cap=16 时同落桶 8，链形 8->24->40->56，可分别删桶头/链中/链尾 */
    private static final int[] CHAIN4 = {8, 24, 40, 56};

    // ---------------- 1. 扰动函数 ----------------

    @Test
    @DisplayName("1. 65536*k 不扰动全落 0 号桶；扰动后散到 0~8")
    void perturbationSpreadsHighBits() {
        for (int k = 0; k <= 8; k++) {
            Integer key = 65536 * k;
            int withoutPerturb = 15 & key.hashCode();
            int withPerturb = MyHashMap.indexFor(MyHashMap.hash(key), 16);
            assertEquals(0, withoutPerturb, "不扰动时低位全 0，k=" + k);
            assertEquals(k, withPerturb, "扰动后应落在 k 号桶，k=" + k);
        }
    }

    // ---------------- 2. 冲突链 ----------------

    @Test
    @DisplayName("2. 三个同桶 key 全部能用「新对象」查回（走 equals 而非 ==）")
    void collisionChainLookupByEqualObject() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        map.put(SAME_BUCKET[0], "A");
        map.put(SAME_BUCKET[1], "B");
        map.put(SAME_BUCKET[2], "C");

        Integer probe = Integer.valueOf(1000);
        assertNotSame(probe, Integer.valueOf(1000), ">>127 的 Integer 每次 valueOf 都是新对象");

        assertEquals("A", map.get(Integer.valueOf(1000)));
        assertEquals("B", map.get(Integer.valueOf(1016)));
        assertEquals("C", map.get(Integer.valueOf(1032)));
        assertNull(map.get(Integer.valueOf(999)), "表里没有的 key 必须返回 null");
    }

    // ---------------- 3. 覆盖写 ----------------

    @Test
    @DisplayName("3. 覆盖链中间的 key：返回旧值，size 不变")
    void overwriteMiddleNode() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        for (int k : SAME_BUCKET) {
            map.put(k, "v" + k);
        }
        assertEquals("v1016", map.put(1016, "新值"));
        assertEquals("新值", map.get(1016));
        assertEquals(3, map.size());
    }

    @Test
    @DisplayName("4. 覆盖桶头节点：返回旧值，size 不变")
    void overwriteHeadNode() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        map.put(2000, "v1");
        assertEquals("v1", map.put(2000, "v2"));
        assertEquals("v2", map.get(2000));
        assertEquals(1, map.size());
    }

    // ---------------- 4. 扩容 ----------------

    @Test
    @DisplayName("5. 容量轨迹：第 13 个 -> 32，第 25 个 -> 64")
    void resizeTrajectory() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        int lastCap = map.table.length;
        assertEquals(16, lastCap, "初始容量 16");
        assertEquals(12, map.threshold, "初始阈值 12");

        for (int i = 0; i < 30; i++) {
            map.put(1000 + i, "v" + i);
            int now = map.table.length;
            if (now != lastCap) {
                int expectedCap = lastCap * 2;
                assertEquals(expectedCap, now, "第 " + (i + 1) + " 个 put 之后应扩到 " + expectedCap);
                lastCap = now;
            }
        }
        assertEquals(64, map.table.length, "30 个元素后应停在 64");
        assertEquals(48, map.threshold, "阈值必须同步成 64*3/4");
        assertEquals(30, map.size());
    }

    @Test
    @DisplayName("6. 扩容后 30 个 key 全部可查回（新对象查，逼 equals）")
    void allKeysSurviveResize() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        for (int i = 0; i < 30; i++) {
            map.put(1000 + i, "v" + i);
        }
        for (int i = 0; i < 30; i++) {
            assertEquals("v" + i, map.get(Integer.valueOf(1000 + i)), "key=" + (1000 + i));
        }
    }

    @Test
    @DisplayName("7. 覆盖写不触发扩容（判断只在 size 真正增加后）")
    void overwriteDoesNotResize() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        for (int i = 0; i < 12; i++) {
            map.put(1000 + i, "v" + i);
        }
        assertEquals(16, map.table.length);
        assertEquals(12, map.size());

        map.put(1000, "覆盖");
        assertEquals(16, map.table.length, "纯覆盖写不该扩容");
        assertEquals(12, map.size());
    }

    // ---------------- 5. remove ----------------

    @Test
    @DisplayName("8. 删桶头（prev == null 那条路）")
    void removeHead() {
        MyHashMap<Integer, String> map = mapWithChain4();
        assertEquals("v8", map.remove(8));
        assertEquals(3, map.size());
        assertNull(map.get(8));
        assertEquals("24->40->56->", dumpBucket(map, 8), "桶头被摘掉后，24 应成为新头");
    }

    @Test
    @DisplayName("9. 删链中间")
    void removeMiddle() {
        MyHashMap<Integer, String> map = mapWithChain4();
        assertEquals("v40", map.remove(40));
        assertEquals(3, map.size());
        assertEquals("8->24->56->", dumpBucket(map, 8));
    }

    @Test
    @DisplayName("10. 删链尾")
    void removeTail() {
        MyHashMap<Integer, String> map = mapWithChain4();
        assertEquals("v56", map.remove(56));
        assertEquals(3, map.size());
        assertEquals("8->24->40->", dumpBucket(map, 8));
    }

    @Test
    @DisplayName("11. 删不存在的 key：返回 null 且 size 不变")
    void removeMissing() {
        MyHashMap<Integer, String> map = mapWithChain4();
        assertNull(map.remove(9999));
        assertEquals(4, map.size());
    }

    @Test
    @DisplayName("12. 删空表里的 key 不能抛（indexFor 传错参数会越界）")
    void removeFromEmpty() {
        MyHashMap<Integer, String> empty = new MyHashMap<>();
        assertNull(empty.remove(1000));
        assertEquals(0, empty.size());
    }

    @Test
    @DisplayName("13. 只扩不缩：删到只剩 1 个，容量不变")
    void noShrink() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        for (int i = 0; i < 30; i++) {
            map.put(1000 + i, "v" + i);
        }
        assertEquals(64, map.table.length);
        for (int i = 0; i < 29; i++) {
            map.remove(1000 + i);
        }
        assertEquals(1, map.size());
        assertEquals(64, map.table.length, "HashMap 不缩容");
        assertEquals(48, map.threshold);
    }

    // ---------------- 6. entrySet / AbstractMap 派生方法 ----------------

    @Test
    @DisplayName("14. entrySet 让整个 map 与 java.util.HashMap 等价（equals + hashCode）")
    void equivalentToJdkHashMap() {
        MyHashMap<Integer, String> mine = new MyHashMap<>();
        java.util.HashMap<Integer, String> jdk = new java.util.HashMap<>();
        for (int i = 0; i < 30; i++) {
            mine.put(1000 + i, "v" + i);
            jdk.put(1000 + i, "v" + i);
        }
        for (int k = 1000; k < 1030; k += 3) {
            assertEquals(jdk.remove(k), mine.remove(k), "remove(" + k + ") 返回值必须一致");
        }
        assertEquals(jdk.size(), mine.size());
        assertTrue(mine.equals(jdk), "equals 依赖 entrySet 迭代器 + Node.equals + remove");
        assertEquals(jdk.hashCode(), mine.hashCode());
    }

    @Test
    @DisplayName("15. entrySet 遍历条数 = size，派生方法全部可用")
    void derivedMethodsWork() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        for (int i = 0; i < 30; i++) {
            map.put(1000 + i, "v" + i);
        }
        int count = 0;
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            assertNotNull(e.getKey());
            count++;
        }
        assertEquals(30, count);
        assertEquals(30, map.keySet().size());
        assertEquals(30, map.values().size());
        assertTrue(map.containsKey(1005));
        assertFalse(map.containsKey(9999));
        assertTrue(map.containsValue("v5"));
        assertFalse(map.containsValue("没这个"));
        assertFalse(map.isEmpty());
        assertNotNull(map.toString(), "toString 依赖 entrySet，之前返回 null 时这里会 NPE");
        assertTrue(new MyHashMap<Integer, String>().isEmpty());
    }

    @Test
    @DisplayName("16. 迭代器耗尽后 next() 抛 NoSuchElementException")
    void iteratorThrowsWhenExhausted() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        map.put(1000, "v0");
        var it = map.entrySet().iterator();
        while (it.hasNext()) {
            it.next();
        }
        assertThrows(NoSuchElementException.class, it::next);
    }

    // ---------------- 7. fail-fast（modCount 三方账本） ----------------

    @Test
    @DisplayName("17. 遍历中 put 新 key -> CME，且抛点在 next() 不在 put()")
    void cmeOnPutNewKey() {
        MyHashMap<Integer, String> map = mapWithFive();

        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> {
            for (Map.Entry<Integer, String> e : map.entrySet()) {
                map.put(9999, "新 key");   // 这一行本身必须不抛
            }
        }, "遍历中新增条目必须抛 CME");
        // 栈顶是迭代器的对账方法：证明责任主体是 next()，不是 put()
        assertEquals("checkForComodification", ex.getStackTrace()[0].getMethodName(),
                "抛点必须在对账方法上，若栈顶是 put 说明记账位置写错了");
        assertEquals("next", ex.getStackTrace()[1].getMethodName(), "对账由 next() 触发");
    }

    @Test
    @DisplayName("18. 遍历中 put 已有 key 换值 -> 不抛（覆盖写不改条目集合）")
    void noCmeOnOverwrite() {
        MyHashMap<Integer, String> map = mapWithFive();

        int count = 0;
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            map.put(1000, "改过的值");     // 覆盖写：不记账，不该抛
            count++;
        }
        assertEquals(5, count, "覆盖写不该打断遍历");
        assertEquals("改过的值", map.get(1000));
        assertEquals(5, map.size(), "覆盖写不改 size");
    }

    @Test
    @DisplayName("19. 遍历中 remove -> CME（摘节点要 --size 且 ++modCount）")
    void cmeOnRemove() {
        MyHashMap<Integer, String> map = mapWithFive();

        assertThrows(ConcurrentModificationException.class, () -> {
            for (Map.Entry<Integer, String> e : map.entrySet()) {
                map.remove(1000);
            }
        }, "遍历中删条目必须抛 CME");
    }

    @Test
    @DisplayName("20. 先对账再判空：耗尽后再改结构，next() 抛 CME 而不是 NoSuchElementException")
    void checkBeforeExhaustedCheck() {
        MyHashMap<Integer, String> map = mapWithFive();
        var it = map.entrySet().iterator();
        while (it.hasNext()) {
            it.next();
        }
        assertThrows(NoSuchElementException.class, it::next, "耗尽且没改结构 -> NoSuchElementException");

        map.put(9999, "新 key");            // 耗尽之后才改结构
        assertThrows(ConcurrentModificationException.class, it::next,
                "两个条件同时成立时必须先报 CME，否则改结构这件事被 NoSuchElementException 掩盖");
    }

    // ---------------- 8. containsKey（2026-11-07 覆写，清 AbstractMap 的 O(n)） ----------------

    @Test
    @DisplayName("21. containsKey 与 get 结论一致：桶头命中 / 链中命中 / 不存在")
    void containsKeyAgreesWithGet() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        for (int k : SAME_BUCKET) {
            map.put(k, "v" + k);
        }

        // 规则 1：用新对象查，别复用 put 时的引用（否则测的是引用比较）
        for (int k : SAME_BUCKET) {
            Integer fresh = Integer.valueOf(k);      // >127，每次都是新对象
            assertTrue(map.containsKey(fresh), "链上已有的 key 必须命中：" + k);
            assertNotNull(map.get(fresh));
        }

        Integer absent = Integer.valueOf(9999);       // 落同一个桶族，但没放进去
        assertFalse(map.containsKey(absent), "没放进去的 key 不能命中");
        assertNull(map.get(absent), "containsKey 与 get 必须同源同结论");
    }

    @Test
    @DisplayName("22. 空表 containsKey 不越界（桶为空的分支）")
    void containsKeyOnEmptyMap() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        assertFalse(map.containsKey(Integer.valueOf(1000)));
        assertFalse(map.containsKey(null));
    }

    @Test
    @DisplayName("23. null key：放着能查到，没放查不到")
    void containsKeyNullKey() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        assertFalse(map.containsKey(null), "空表里没有 null key");

        map.put(null, "nullVal");
        assertTrue(map.containsKey(null), "put 过 null key 就该命中");
        assertEquals("nullVal", map.get(null));

        map.remove(null);
        assertFalse(map.containsKey(null), "remove 之后不能再命中");
    }

    @Test
    @DisplayName("24. 与 JDK HashMap 逐 key 对照 containsKey 结论（key 必须 > 127）")
    void containsKeyEquivalentToJdk() {
        MyHashMap<Integer, String> mine = new MyHashMap<>();
        Map<Integer, String> jdk = new java.util.HashMap<>();
        // 先真的把 100 个 key 放进去。第一版这里只 put 了 CHAIN4，却拿 1000+16i 去查 ——
        // 两边都返回 false，用例空转，故意改坏也不红。
        for (int i = 0; i < 100; i++) {
            int k = 1000 + 16 * i;             // 同桶族、> 127、值各不相同
            mine.put(k, "v" + k);
            jdk.put(k, "v" + k);
        }
        // 对照查询：命中与不命中都要覆盖
        for (int i = 0; i < 100; i++) {
            int k = 1000 + 16 * i;
            assertEquals(jdk.containsKey(k), mine.containsKey(k), "命中 key=" + k + " 结论不一致");
        }
        for (int k : new int[]{9999, 12345, 65535, 8}) {
            assertEquals(jdk.containsKey(k), mine.containsKey(k), "不命中 key=" + k + " 结论不一致");
        }
    }

    // ---------------- 工具 ----------------

    private static MyHashMap<Integer, String> mapWithFive() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        for (int i = 0; i < 5; i++) {
            map.put(1000 + i, "v" + i);
        }
        assertEquals(5, map.size());
        assertEquals(16, map.table.length, "5 个元素不该触发扩容");
        return map;
    }

    private static MyHashMap<Integer, String> mapWithChain4() {
        MyHashMap<Integer, String> map = new MyHashMap<>();
        for (int k : CHAIN4) {
            map.put(k, "v" + k);
        }
        assertEquals(16, map.table.length, "4 个元素不该触发扩容");
        assertEquals("8->24->40->56->", dumpBucket(map, 8), "前置条件：四个 key 必须同桶且保序");
        return map;
    }

    private static String dumpBucket(MyHashMap<Integer, String> map, int bucket) {
        StringBuilder sb = new StringBuilder();
        MyHashMap.Node<Integer, String> e = map.table[bucket];
        while (e != null) {
            sb.append(e.key).append("->");
            e = e.next;
        }
        return sb.toString();
    }
}
