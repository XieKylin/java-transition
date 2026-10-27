package com.study.map;

/**
 * Day-05 验收用例
 * 1. 扰动对照：证明 ^ (h >>> 16) 确实起作用
 * 2. 冲突链：三个同下标 key 都能取回（逼它走 equals，不走 ==）
 * 3. 覆盖写：表头节点 / 链中间节点，size 不增加
 */
public class MyHashMapDemo {

    public static void main(String[] args) {
        perturbation();
        collisionAndOverwrite();
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
}
