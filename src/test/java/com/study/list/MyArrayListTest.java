package com.study.list;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyArrayList 回归测试。
 *
 * 来源：原 target/review/Review.java 的 14 个手工用例（那是 mvn clean 就消失的临时目录）。
 * 迁移时把「打印 + 人眼比对」换成了 assertEquals / assertThrows —— 失败时框架会直接告诉你
 * 期望值和实际值，不用回去翻输出。
 */
class MyArrayListTest {

    private static String dump(MyArrayList<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    @DisplayName("1. 10 万次尾插后 size = 100000（扩容过程不丢元素）")
    void tailAdd100k() {
        MyArrayList<String> a = new MyArrayList<>();
        for (int i = 0; i < 100_000; i++) {
            a.add("v");
        }
        assertEquals(100_000, a.size());
    }

    @Test
    @DisplayName("2. 三次头插 -> cba（每次插入都要整体后移）")
    void headAddKeepsOrder() {
        MyArrayList<String> b = new MyArrayList<>();
        b.add("a");
        b.add(0, "b");
        b.add(0, "c");
        assertEquals("cba", dump(b));
    }

    @Test
    @DisplayName("3. 尾插与指定位置插入混用 -> aXbc")
    void mixedAdd() {
        MyArrayList<String> c = new MyArrayList<>();
        c.add("a");
        c.add("b");
        c.add(1, "X");
        c.add("c");
        assertEquals("aXbc", dump(c));
    }

    @Test
    @DisplayName("4. 多次指定位置插入 -> 1a2b3")
    void multiInsert() {
        MyArrayList<String> d = new MyArrayList<>();
        d.add("1");
        d.add("2");
        d.add("3");
        d.add(1, "a");
        d.add(3, "b");
        assertEquals("1a2b3", dump(d));
    }

    @Test
    @DisplayName("5. 空集合的迭代器一次都不进循环")
    void emptyIteration() {
        Iterator<String> it = new MyArrayList<String>().iterator();
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("6. 耗尽后 next() 抛 NoSuchElementException")
    void nextAfterExhausted() {
        MyArrayList<String> f = new MyArrayList<>();
        f.add("x");
        Iterator<String> it = f.iterator();
        it.next();
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    @DisplayName("7. 两个迭代器互不干扰")
    void independentIterators() {
        MyArrayList<String> g = new MyArrayList<>();
        g.add("1");
        g.add("2");
        g.add("3");
        Iterator<String> i1 = g.iterator();
        Iterator<String> i2 = g.iterator();
        i1.next();                                  // i1 走到 "2"
        assertEquals("1", i2.next());               // i2 仍从 "1" 开始
        assertEquals("2", i1.next());
    }

    @Test
    @DisplayName("8. get 下标上界越界")
    void getOutOfUpperBound() {
        MyArrayList<String> c = threeElements();
        assertThrows(IndexOutOfBoundsException.class, () -> c.get(99));
    }

    @Test
    @DisplayName("9. get 负数下标越界")
    void getNegative() {
        MyArrayList<String> c = threeElements();
        assertThrows(IndexOutOfBoundsException.class, () -> c.get(-1));
    }

    @Test
    @DisplayName("10. add 下标上界越界")
    void addOutOfUpperBound() {
        MyArrayList<String> c = threeElements();
        assertThrows(IndexOutOfBoundsException.class, () -> c.add(99, "z"));
    }

    @Test
    @DisplayName("11. add 负数下标越界")
    void addNegative() {
        MyArrayList<String> c = threeElements();
        assertThrows(IndexOutOfBoundsException.class, () -> c.add(-1, "z"));
    }

    /** get(size) 是最容易写错的边界：容易静默返回 null 而不是抛。 */
    @Test
    @DisplayName("12. get(size) 必须抛，不能静默返回 null")
    void getAtSizeMustThrow() {
        MyArrayList<String> h = new MyArrayList<>();
        h.add("a");
        assertThrows(IndexOutOfBoundsException.class, () -> h.get(h.size()));
    }

    /** add(size, e) 和 get(size) 不同：它是合法的尾插。 */
    @Test
    @DisplayName("13. add(size, e) 是合法尾插")
    void addAtSizeIsLegal() {
        MyArrayList<String> h = new MyArrayList<>();
        h.add("a");
        assertDoesNotThrow(() -> h.add(h.size(), "z"));
    }

    @Test
    @DisplayName("14. 尾插后顺序不变")
    void orderAfterTailAdd() {
        MyArrayList<String> h = new MyArrayList<>();
        h.add("a");
        h.add(h.size(), "z");
        assertEquals("az", dump(h));
    }

    private static MyArrayList<String> threeElements() {
        MyArrayList<String> c = new MyArrayList<>();
        c.add("a");
        c.add("X");
        c.add("b");
        c.add("c");
        return c;
    }
}
