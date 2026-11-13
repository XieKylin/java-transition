import java.util.ArrayList;
import java.util.List;

/**
 * Day-21 泛型 ① 故意编不过的对照实验（不参与 maven，本目录不编译）。
 *
 * <p>用 JDK 全路径单文件编译，把报错原文落到 playground/pecs-error.txt：
 * <pre>
 * D:/developkit/jdk21/bin/javac -d playground/out playground/PecsFail.java 2&gt;&amp;1 | tee playground/pecs-error.txt
 * </pre>
 *
 * <p>C/D/B 反例/F 四段各占一个方法，是为了让 javac 一次把 4 条报错全报出来，
 * 且每条报错行号能直接对到段号。
 */
public class PecsFail {

    /** C 段：把 PECS 反过来写 —— 消费者放 extends 位、生产者放 super 位。 */
    static void drain2(List<? super Number> src, List<? extends Number> dst) {
        dst.add(src.get(0));   // 两个独立的 capture，互不相干
    }

    /** D 段：同一个变量，取出再放回，能不能编过？ */
    static void roundTrip(List<? extends Number> a) {
        a.add(a.get(0));
    }

    /** B 反例：extends 侧不接受具体值。 */
    static void addConcrete(List<? extends Number> src) {
        src.add(1);
    }

    /** G 段：两个独立的 ? extends 参数互相倒 —— 验证"每个 ? 新造一个 CAP#"是否导致额外报错。 */
    static void cross(List<? extends Number> a, List<? extends Number> b) {
        a.add(b.get(0));
    }

    /**
     * H 段：真正让两个 CAP# 正面相撞的写法 —— 泛型方法要求两个实参推出同一个 T。
     * G 段只是"上界对不上捕获"，H 段才是"CAP#1 对 CAP#2"。
     */
    static <T> void same(List<T> x, List<T> y) {
    }

    static void captureCollision(List<? extends Number> a, List<? extends Number> b) {
        same(a, b);
    }

    /** F 段：泛型不变 —— List&lt;String&gt; 不是 List&lt;Object&gt; 的子类型。 */
    static void invariant() {
        List<Object> list = new ArrayList<String>();
    }
}
