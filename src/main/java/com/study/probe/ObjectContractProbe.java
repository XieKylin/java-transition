package com.study.probe;

import java.util.*;

/**
 * Object 契约验证：equals / hashCode 的五种破坏方式
 *
 * 编译运行：
 *   javac -encoding UTF-8 -d target/probe src/main/java/com/study/probe/ObjectContractProbe.java
 *   java -cp target/probe com.study.probe.ObjectContractProbe
 */
public class ObjectContractProbe {

    // ===== 场景 1：只重写 equals，不重写 hashCode =====
    static class OnlyEquals {
        final String name;
        static int eqCalls = 0;          // 自证用：equals 到底被调用了几次
        OnlyEquals(String name) { this.name = name; }
        @Override public boolean equals(Object o) {
            eqCalls++;
            System.out.println("        [OnlyEquals.equals 第 " + eqCalls + " 次被调用] " + name + " vs "
                    + (o instanceof OnlyEquals ? ((OnlyEquals) o).name : o));
            return o instanceof OnlyEquals && name.equals(((OnlyEquals) o).name);
        }
        @Override public String toString() { return "OnlyEquals(" + name + ")"; }
    }

    // ===== 场景 1b 的对照：两个都正确重写 =====
    static class BothOk {
        final String name;
        static int eqCalls = 0;
        BothOk(String name) { this.name = name; }
        @Override public boolean equals(Object o) {
            eqCalls++;
            return o instanceof BothOk && name.equals(((BothOk) o).name);
        }
        @Override public int hashCode() { return name.hashCode(); }   // 关键：只多了这一行
        @Override public String toString() { return "BothOk(" + name + ")"; }
    }

    // ===== 场景 2：两个都重写，但字段可变 =====
    static class MutableKey {
        int v;
        MutableKey(int v) { this.v = v; }
        @Override public boolean equals(Object o) {
            return o instanceof MutableKey && ((MutableKey) o).v == v;
        }
        @Override public int hashCode() { return Objects.hash(v); }
        @Override public String toString() { return "K(" + v + ")"; }
    }

    // ===== 场景 3：破坏对称性 =====
    static class A {
        @Override public boolean equals(Object o) { return o instanceof B || o instanceof A; }
        @Override public int hashCode() { return 1; }
        @Override public String toString() { return "A"; }
    }
    static class B {
        // 沿用 Object.equals：只有同一引用才 true
        @Override public int hashCode() { return 1; }
        @Override public String toString() { return "B"; }
    }

    // ===== 场景 4：hashCode 全相同，equals 不同（合法碰撞） =====
    static class Collide {
        final int id;
        Collide(int id) { this.id = id; }
        @Override public boolean equals(Object o) {
            return o instanceof Collide && ((Collide) o).id == id;
        }
        @Override public int hashCode() { return 1; }   // 全部塞进同一个桶
        @Override public String toString() { return "C(" + id + ")"; }
    }

    static void title(String s) {
        System.out.println("\n========== " + s + " ==========");
    }

    public static void main(String[] args) {
        // ---------- 场景 1 ----------
        title("场景1  只重写 equals，不重写 hashCode");
        Map<OnlyEquals, String> m1 = new HashMap<>();
        OnlyEquals k1 = new OnlyEquals("tom");
        m1.put(k1, "工程师");
        System.out.println("  m1.get(同一个引用 k1)            : " + m1.get(k1));
        System.out.println("  m1.get(new OnlyEquals(\"tom\"))   : " + m1.get(new OnlyEquals("tom")));
        System.out.println("  m1.size()                       : " + m1.size());
        System.out.println("  >>> OnlyEquals.equals 累计被调用次数 = " + OnlyEquals.eqCalls
                + "   （第一个 get 靠 == 短路，第二个 get 桶没命中）");

        // ---------- 场景 1b（对照） ----------
        title("场景1b 对照组：只多一行 hashCode，其余完全相同");
        Map<BothOk, String> m1b = new HashMap<>();
        m1b.put(new BothOk("tom"), "工程师");
        System.out.println("  m1b.get(new BothOk(\"tom\"))  : " + m1b.get(new BothOk("tom")));
        System.out.println("  >>> BothOk.equals 被调用次数  = " + BothOk.eqCalls
                + "   （桶命中 → 进入第二段 → equals 真的跑了）");

        // ---------- 场景 2 ----------
        title("场景2  两个都重写，但 key 的字段被改了");
        Map<MutableKey, String> m2 = new HashMap<>();
        MutableKey mk = new MutableKey(1);
        m2.put(mk, "值");
        System.out.println("  改之前 get : " + m2.get(mk) + "   size=" + m2.size());
        mk.v = 2;   // 对象自己变了，map 里的账本没变
        System.out.println("  把 v 改成 2 之后：");
        System.out.println("    get(k)     : " + m2.get(mk));
        System.out.println("    remove(k)  : " + m2.remove(mk));
        System.out.println("    size       : " + m2.size() + "   <<< 删不掉了");
        System.out.println("  用新值造一个同值 key : " + m2.get(new MutableKey(2)));
        System.out.println("  遍历看看里面到底还有啥:");
        for (Map.Entry<MutableKey, String> e : m2.entrySet()) {
            System.out.println("    entry = " + e.getKey() + " -> " + e.getValue());
        }

        // ---------- 场景 3 ----------
        title("场景3  破坏对称性：A.equals(B)=true，但 B.equals(A)=false");
        System.out.println("  new A().equals(new B()) : " + new A().equals(new B()));
        System.out.println("  new B().equals(new A()) : " + new B().equals(new A()));
        Set<Object> s3 = new HashSet<>();
        s3.add(new A());
        System.out.println("  HashSet 里有 A，contains(new B()) : " + s3.contains(new B()));
        Set<Object> s3b = new HashSet<>();
        s3b.add(new B());
        System.out.println("  HashSet 里有 B，contains(new A()) : " + s3b.contains(new A()));
        List<Object> l3 = new ArrayList<>();
        l3.add(new A());
        System.out.println("  ArrayList[A].contains(new B())    : " + l3.contains(new B()));

        // ---------- 场景 4 ----------
        title("场景4  hashCode 全相同，equals 不同（合法的哈希碰撞）");
        Map<Collide, String> m4 = new HashMap<>();
        m4.put(new Collide(1), "one");
        m4.put(new Collide(2), "two");
        m4.put(new Collide(3), "three");
        System.out.println("  size = " + m4.size());
        System.out.println("  get(C(2)) = " + m4.get(new Collide(2)) + "   （三者同桶，靠 equals 挑选）");

        // ---------- 场景 5 ----------
        title("场景5  null 的两副面孔");
        System.out.println("  new Collide(1).equals(null) : " + new Collide(1).equals(null));
        Map<String, String> m5 = new HashMap<>();
        m5.put(null, "null 是合法 key");
        System.out.println("  map.put(null,..) / map.get(null) : " + m5.get(null));
        try {
            new Collide(1).hashCode();
            System.out.println("  hashCode() 在 null 字段场景下不抛（本例无 null 字段）");
        } catch (NullPointerException e) {
            System.out.println("  hashCode() 抛 NPE");
        }

        // ---------- 附加：String 的正确姿势 ----------
        title("附加  String 为什么能当 key");
        System.out.println("  new String(\"a\").equals(new String(\"a\")) : "
                + new String("a").equals(new String("a")));
        System.out.println("  new String(\"a\").hashCode() == 同值      : "
                + (new String("a").hashCode() == new String("a").hashCode()));
    }
}
