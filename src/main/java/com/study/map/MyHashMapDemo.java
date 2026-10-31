package com.study.map;

/**
 * MyHashMap 验收用例
 * 1. 扰动对照：证明 ^ (h >>> 16) 确实起作用
 * 2. 冲突链：三个同下标 key 都能取回（逼它走 equals，不走 ==）
 * 3. 覆盖写：表头节点 / 链中间节点，size 不增加
 * 4. 扩容轨迹：13 个 -> 16/32，25 个 -> 32/64；30 key 全量校验 + 不变式断言
 */
public class MyHashMapDemo {

    public static void main(String[] args) {
        perturbation();
        collisionAndOverwrite();
        resizeTrajectory();
        removeCase();
    }

    /**
     * 65536*k 的低 16 位全是 0，所以不扰动必然全部落在 0 号桶。
     * 扰动后 h >>> 16 把高位移下来，才能散到 0~8。
     */
    private static void perturbation() {
        System.out.println("【1】扰动对照    key = 65536*k");
        for (int k = 0; k <= 8; k++) {
            Integer key = 65536 * k;
            int raw = 15 & key.hashCode();
            int idx = MyHashMap.indexFor(MyHashMap.hash(key), 16);
            System.out.println("  k=" + k + "  hashCode=" + key.hashCode()
                    + "  不扰动=" + raw + "  扰动后=" + idx);
        }
        System.out.println();
    }

    private static void collisionAndOverwrite() {
        MyHashMap<Integer, String> map = new MyHashMap<>();

        Integer a = Integer.valueOf(1000);
        Integer b = Integer.valueOf(1016);
        Integer c = Integer.valueOf(1032);

        System.out.println("【2】冲突链");
        System.out.println("  三个 key 的下标: "
                + MyHashMap.indexFor(MyHashMap.hash(a), 16) + " / "
                + MyHashMap.indexFor(MyHashMap.hash(b), 16) + " / "
                + MyHashMap.indexFor(MyHashMap.hash(c), 16));

        map.put(a, "A");
        map.put(b, "B");
        map.put(c, "C");

        Integer probe = Integer.valueOf(1000);
        System.out.println("  probe 与 a 是同一对象? " + (probe == a) + "   <- 必须是 false，否则测不到 equals");
        System.out.println("  get(1000 的新对象) = " + map.get(probe));
        System.out.println("  get(1016 的新对象) = " + map.get(Integer.valueOf(1016)));
        System.out.println("  get(1032 的新对象) = " + map.get(Integer.valueOf(1032)));
        System.out.println("  get(表里没有的 999) = " + map.get(Integer.valueOf(999)));
        System.out.println();

        System.out.println("【3】覆盖写");
        String oldMid = map.put(Integer.valueOf(1016), "B2");
        System.out.println("  覆盖链中间 1016: 返回旧值=" + oldMid
                + "  get=" + map.get(Integer.valueOf(1016)));
        System.out.println("  size 应仍为 3 -> " + map.size());
        System.out.println();

        MyHashMap<Integer, String> one = new MyHashMap<>();
        one.put(Integer.valueOf(2000), "v1");
        String oldHead = one.put(Integer.valueOf(2000), "v2");
        System.out.println("  覆盖表头 2000: 返回旧值=" + oldHead
                + "  get=" + one.get(Integer.valueOf(2000)));
        System.out.println("  size 应仍为 1 -> " + one.size());
    }

    /**
     * 【4】扩容轨迹 + 30 key 全量校验
     *
     * 为什么 key 用 1000..1029 而不是 0..29：
     * Integer 缓存只覆盖 -128~127，用 0..29 的话 put 和 get 拿到的是同一个缓存实例，
     * get 里 `key == e.getKey()` 会短路命中，equals 那条路径永远测不到（Day-05 踩过）。
     */
    private static void resizeTrajectory() {
        System.out.println("【4】扩容轨迹");
        MyHashMap<Integer, String> map = new MyHashMap<>();
        int lastCap = map.table.length;
        StringBuilder trace = new StringBuilder(String.valueOf(lastCap));

        for (int i = 0; i < 30; i++) {
            map.put(1000 + i, "v" + i);
            if (map.table.length != lastCap) {
                trace.append(" --第").append(i + 1).append("个--> ").append(map.table.length);
                lastCap = map.table.length;
            }
        }
        System.out.println("  轨迹: " + trace);
        System.out.println("  期望: 16 --第13个--> 32 --第25个--> 64");
        System.out.println("  终态: cap=" + map.table.length + " size=" + map.size()
                + " threshold=" + map.threshold);

        int miss = 0;
        for (int i = 0; i < 30; i++) {
            if (map.get(Integer.valueOf(1000 + i)) == null) miss++;
        }
        System.out.println("  丢失 key（每次用新对象查）: " + miss);

        // 不变式断言：一次同时抓住两种阈值漏改
        //   只改 table 不改 threshold -> 每插一个就翻倍，cap 会远大于 64
        //   只改 threshold 不改 table -> cap 停在 16，桶永远不够用
        boolean ok = map.table.length == 64
                && map.threshold == 48
                && map.size() == 30
                && miss == 0;
        System.out.println("  不变式 [cap=64 && threshold=48 && size=30 && 丢失=0] -> "
                + (ok ? "PASS" : "FAIL（先查 resizeBrute 末尾有没有同步 threshold）"));
    }

    /**
     * 【5】remove：同桶四连的三种位置 + JDK 对照 + 只扩不缩
     *
     * 8/24/40/56 在 cap=16 时全落桶 8，链是 8->24->40->56，
     * 正好能分别覆盖「删桶头(prev==null) / 删链中 / 删链尾」三条路径。
     */
    private static void removeCase() {
        System.out.println();
        System.out.println("【5】remove");
        String[] labels = {"删桶头 8", "删链中 40", "删链尾 56"};
        int[] targets = {8, 40, 56};
        String[] expect = {"24->40->56->", "8->24->56->", "8->24->40->"};

        for (int c = 0; c < targets.length; c++) {
            MyHashMap<Integer, String> m = new MyHashMap<>();
            for (int k : new int[]{8, 24, 40, 56}) {
                m.put(k, "v" + k);
            }
            String old = m.remove(targets[c]);
            String chain = dumpBucket(m, 8);
            System.out.println("  " + labels[c] + ": 返回=" + old + " size=" + m.size()
                    + "  链=" + chain + "  " + (chain.equals(expect[c]) ? "PASS" : "FAIL(期望 " + expect[c] + ")"));
        }

        // JDK 对照
        MyHashMap<Integer, String> mine = new MyHashMap<>();
        java.util.HashMap<Integer, String> jdk = new java.util.HashMap<>();
        for (int i = 0; i < 30; i++) {
            mine.put(1000 + i, "v" + i);
            jdk.put(1000 + i, "v" + i);
        }
        for (int k = 1000; k < 1030; k += 3) {
            mine.remove(k);
            jdk.remove(k);
        }
        System.out.println("  equals(jdk)=" + mine.equals(jdk)
                + "  size=" + mine.size() + "/" + jdk.size()
                + "  get(删掉的 1000)=" + mine.get(1000)
                + "  " + (mine.equals(jdk) && mine.get(1000) == null ? "PASS" : "FAIL"));

        // 只扩不缩：删到只剩 1 个，cap 不该变
        int capBefore = mine.table.length;
        for (int i = 0; i < 30; i++) {
            mine.remove(1000 + i);
        }
        System.out.println("  删空后: size=" + mine.size() + " cap=" + mine.table.length
                + "（删前 " + capBefore + "）  "
                + (mine.table.length == capBefore && mine.isEmpty() ? "PASS 只扩不缩" : "FAIL"));
    }

    private static String dumpBucket(MyHashMap<Integer, String> map, int i) {
        StringBuilder sb = new StringBuilder();
        MyHashMap.Node<Integer, String> e = map.table[i];
        while (e != null) {
            sb.append(e.key).append("->");
            e = e.next;
        }
        return sb.toString();
    }
}
