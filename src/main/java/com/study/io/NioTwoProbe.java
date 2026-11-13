package com.study.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * Day-21 闭环 3：NIO.2 —— Path / Files 与流式遍历。
 *
 * <p>三个要点：
 * <ol>
 *   <li>Path 用 Paths.get("a","b") 拼，交给系统决定分隔符，不用手写 "/" 或 "\"</li>
 *   <li>Files.walk / Files.list 返回的 Stream&lt;Path&gt; 必须 try-with-resources —— 流背后挂着目录句柄</li>
 *   <li>readString / writeString 是 JDK 11+ 的一步到位写法，省掉 Reader/Writer 样板</li>
 * </ol>
 */
public final class NioTwoProbe {

    private NioTwoProbe() {
    }

    public static void main(String[] args) throws IOException {
        Path root = Paths.get("src", "main", "java");

        // 1) walk：递归遍历，必须关流
        long javaCount;
        long entryCount;
        try (Stream<Path> s = Files.walk(root)) {
            List<Path> all = s.toList();
            entryCount = all.size();
            javaCount = all.stream().filter(p -> p.toString().endsWith(".java")).count();
            all.stream().filter(p -> p.toString().endsWith(".java")).limit(3)
                    .forEach(p -> System.out.println("  样本: " + p));
        }
        System.out.println("1) walk 条目总数 = " + entryCount + "，.java 文件数 = " + javaCount);

        // 2) list：只看一层，同样必须关流
        try (Stream<Path> s = Files.list(root)) {
            System.out.println("2) list 一层条目数 = " + s.count() + "（walk 的 " + entryCount + " 是递归结果）");
        }

        // 3) readString / writeString / readAllBytes / size
        Path tmp = Files.createTempFile("niotwo", ".txt");
        try {
            Files.writeString(tmp, "第一行\n第二行\n");
            String content = Files.readString(tmp);
            System.out.println("3) readString 行数 = " + content.lines().count()
                    + "，Files.size = " + Files.size(tmp) + " 字节"
                    + "，readAllBytes.length = " + Files.readAllBytes(tmp).length);
        } finally {
            Files.deleteIfExists(tmp);
        }

        // 4) copy / move：REPLACE_EXISTING 必须显式给，默认存在就抛 FileAlreadyExistsException
        Path src = Files.createTempFile("niotwo-src", ".txt");
        Path dst = Paths.get(src.toString() + ".copy");
        try {
            Files.writeString(src, "abc");
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("4) copy 后目标存在 = " + Files.exists(dst)
                    + "，内容 = " + Files.readString(dst));
        } finally {
            Files.deleteIfExists(src);
            Files.deleteIfExists(dst);
        }

        // 5) Path vs File：normalize / resolve 是 File 没有的能力
        Path p = Paths.get("src", "main", "java", "..", "java", "com", "study");
        System.out.println("5) normalize = " + p.normalize());
        System.out.println("5) resolve   = " + root.resolve("com/study/io"));
    }
}
