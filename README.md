# java-transition

一个 Java 后端转型期的**手写实现库**。不写 demo，只写能跑通、有测试、能讲清原理的东西。

所有实现都对照 JDK 源码，并用 JUnit 5 固定住行为边界——包括那些"看起来对、其实错"的边界。

```bash
mvn clean test      # 32 个用例，全绿
```

## 为什么有这个仓库

正在做一份 12 个月的 Java 补课计划（2026-10 起）。这个仓库是它的代码侧存档：每个知识点都要落到「能编译、能跑、有用例、能复述」四件事上才算数。

不是教程，是作业本。所以你会看到 `MyArrayList` 只实现了 `add/get/grow/iterator`，而不是完整 API——只写能讲清楚的部分。

## 已实现

### `com.study.list.MyArrayList`

| 项 | 说明 |
|---|---|
| 扩容 | `oldCap + (oldCap >> 1)`，即 1.5 倍；`oldCapacity == 0` 特判给 10 |
| 迭代器 | `Itr` 三字段 + `modCount` / `expectedModCount` fail-fast |
| 测试 | 14 个用例，含 10 万次 add 的容量轨迹与边界 |

### `com.study.map.MyHashMap`

| 项 | 说明 |
|---|---|
| 结构 | 数组 + 单链表（**不含红黑树**，树化在 JDK 8+ 的 `treeifyBin`，不在本仓库范围） |
| hash | `h = key.hashCode() ^ (h >>> 16)` 扰动 |
| 取桶 | `(n - 1) & hash`，**前提是用 2 的幂**，函数注释里写死了这条 |
| 扩容 | 2 倍 + 高低位拆分；尾插保序版（JDK 8 风格，非 JDK 7 头插） |
| 视图 | `entrySet()` 惰性定位迭代器，继承 `AbstractMap` 白拿 13 个方法 |
| 测试 | 16 个用例，含 `equals(jdkHashMap)` 终态对照 |

两个实现共用一条验收原则：**拿 JDK 当 oracle，比终态不比过程**。

## 目录

```
src/main/java/com/study/
├── list/       MyArrayList + Demo
├── map/        MyHashMap + Demo（5 段验收）
└── probe/      行为验证片段，不为功能服务
                ├── CmeProbe             四场景：CME 抛在哪一步
                ├── MapIterProbe         四场景：三种视图与三个迭代器
                └── ObjectContractProbe  六场景：equals/hashCode 契约
src/test/java/com/study/
├── list/       MyArrayListTest   14 用例
└── map/        MyHashMapTest     16 用例
```

`probe/` 是刻意的：每个结论配一段能跑出反例的代码，不用肉眼比对日志。凡是"某方法是否被调用"，一律用计数器自证。

## 系列文章

- [为什么 ArrayList 是 1.5 倍扩容？我压测了 500 万次](待发布)
- 一个 `e.next = null` 的缺失，如何让 `HashMap` 死循环（待写）

## 工作纪律

| 纪律 | 起因 |
|---|---|
| commit 完紧接着 `git push`，当成一个动作 | 曾积压 3 个未推提交，GitHub 连续几天无活跃记录 |
| 测试不进 `target/` | `target/review/` 在 gitignore 里，`mvn clean` 就没了 |
| 一个缺失必须构造能分别触发每个结局的用例 | 断尾 `e.next = null` 缺失有两个结局：死循环 vs 静默串桶，只测出一个等于判另一个不存在 |
