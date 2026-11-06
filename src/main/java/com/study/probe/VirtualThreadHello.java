package com.study.probe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 探针：JDK 21 虚拟线程最小验证（M2 前置）。
 *
 * 只看三件事：
 *   ① Thread.ofVirtual() 能起线程、能 join、能拿返回值
 *   ② 在虚拟线程里 isVirtual() 为 true，平台线程为 false
 *   ③ 一万个虚拟线程同时驻留不炸 —— 因为它们是 Java 对象，不是 1:1 的 OS 线程
 *
 * 运行：java -cp target/classes com.study.probe.VirtualThreadHello
 */
public class VirtualThreadHello {

    public static void main(String[] args) throws Exception {
        System.out.println("JVM: " + System.getProperty("java.version"));

        // ① 起一个，join 回来
        Thread t = Thread.ofVirtual().name("vt-hello").start(() -> {
            System.out.println("[①] 跑在虚拟线程里：name=" + Thread.currentThread().getName()
                    + "  isVirtual=" + Thread.currentThread().isVirtual());
        });
        t.join();
        System.out.println("[①] join 之后：isAlive=" + t.isAlive()
                + "  isVirtual=" + t.isVirtual());
        System.out.println("[①] 主线程 isVirtual=" + Thread.currentThread().isVirtual());

        // ② 一万个虚拟线程同时驻留（不 join，全部挂起在 sleep 上）
        AtomicInteger done = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            threads.add(Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                done.incrementAndGet();
            }));
        }
        System.out.println("[②] 已启动 10000 个虚拟线程，此刻存活 "
                + threads.stream().filter(Thread::isAlive).count());
        for (Thread vt : threads) {
            vt.join();
        }
        System.out.println("[②] 全部 join 完成，done=" + done.get());

        // ③ 同样一万个平台线程做对照 —— 不真跑，只说明上限在哪
        System.out.println("[③] 对照：平台线程每个对应一个 OS 线程，"
                + "一万个要预分配约 1 万个栈（默认 512KB~1MB 保留）");
    }
}
