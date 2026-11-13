import java.util.List;

/**
 * Day-21 泛型 ② 擦除的边界 —— 四条"不能"各一行，让编译器当证据。
 *
 * <p>编译方式（本文件不参与 maven）：
 * <pre>
 * D:/developkit/jdk21/bin/javac -J-Duser.language=en -J-Dstdout.encoding=UTF-8 \
 *     -encoding UTF-8 -d playground/out playground/EraseFail.java 2&gt;&amp;1 | tee playground/erase-error.txt
 * </pre>
 */
public class EraseFail<T> {

    // ① 不能 new T[] —— 数组分配需要在运行期知道元素具体类型，擦除后 T 只剩上界 Object
    T[] arr = new T[10];

    // ② 不能 instanceof T —— 擦除后没有类型信息可查
    static <U> boolean isU(Object o) {
        return o instanceof U;
    }

    // ②b 不能 instanceof 参数化类型 —— 运行期只剩原始类型 List
    static boolean isListOfString(Object o) {
        return o instanceof List<String>;
    }

    // ③ 静态成员不能用类的类型参数 —— 静态成员先于实例存在，拿不到实例化时的 T
    static T staticFoo() {
        return null;
    }

    // ④ 不能靠参数化类型重载 —— 擦除后两个签名都是 f(List)
    static void f(List<String> x) {
    }

    static void f(List<Integer> x) {
    }
}
