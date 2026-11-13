package com.study.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IO 地基实验（Day-21 补课）：打开 → 读写 → 关闭。
 *
 * <p>四段各回答一个问题：
 * <ol>
 *   <li>字节管读出来的东西长什么样（原始 byte，0~255）</li>
 *   <li>字符管读出来的东西长什么样（已经解码成 char，一个中文 = 1 个 char）</li>
 *   <li>不关流，这个文件还能不能被删除 / 改名（句柄还在手上）</li>
 *   <li>关掉之后能不能删（句柄还回去了）</li>
 * </ol>
 */
public final class IoGroundProbe {

    private IoGroundProbe() {
    }

    public static void main(String[] args) throws IOException {
        Path p = Files.createTempFile("io-ground", ".txt");
        Files.writeString(p, "第一行\n", StandardCharsets.UTF_8);
        System.out.println("文件内容 = 「第一行」+ 换行，UTF-8 编码，磁盘上共 "
                + Files.size(p) + " 字节");

        // 1) 字节管：一次一个 byte，读出来是 0~255 的 int
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[16];
            int n = in.read(buf);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                sb.append(buf[i]).append(' ');
            }
            System.out.println("1) 字节管：读到 " + n + " 个字节，原始值 = " + sb.toString().trim());
        }

        // 2) 字符管：编码层在管子外面替你把 byte 解码成 char
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            char[] buf = new char[16];
            int n = r.read(buf);
            String s = new String(buf, 0, n);
            System.out.println("2) 字符管：读到 " + n + " 个 char，内容 = ["
                    + s.replace("\n", "\\n") + "]");
        }

        // 3) 故意不关：句柄还在进程手里，看系统拦不拦
        InputStream leak = Files.newInputStream(p);
        int first = leak.read();
        System.out.println("3) 打开后故意不关，先读了一个字节 = " + first);
        try {
            Files.delete(p);
            System.out.println("3) 结果：不关流也删掉了 —— 本平台没拦住（Linux 上表现为句柄耗尽才报错）");
        } catch (Exception e) {
            System.out.println("3) 结果：删不掉 → " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // 4) 关掉再删
        leak.close();
        Files.deleteIfExists(p);
        System.out.println("4) close() 之后再删：成功，文件仍存在 = " + Files.exists(p));

        // 5) 关流最硬的证据：写流不关，数据还在 Java 缓冲区里，磁盘上什么都没有
        Path dirty = Files.createTempFile("dirty", ".txt");
        java.io.BufferedOutputStream out =
                new java.io.BufferedOutputStream(Files.newOutputStream(dirty));
        out.write("HELLO".getBytes(StandardCharsets.UTF_8));
        System.out.println("5) 写了 5 个字节但没关流：磁盘上文件大小 = " + Files.size(dirty));
        out.close();
        System.out.println("5) close() 之后：文件大小 = " + Files.size(dirty)
                + "，内容 = [" + Files.readString(dirty) + "]");
        Files.deleteIfExists(dirty);
    }
}
