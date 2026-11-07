package com.study.probe;

import java.nio.ByteBuffer;

/**
 * Day-16 probe #3: what is mark, when is it discarded, and what reset() costs.
 * Buffer.mark is private with no getter, so everything here is observed behaviourally.
 */
public class MarkProbe {

    static void p(String tag, ByteBuffer b) {
        System.out.printf("%-34s cap=%2d pos=%2d lim=%2d rem=%2d%n",
                tag, b.capacity(), b.position(), b.limit(), b.remaining());
    }

    static void tryReset(String tag, ByteBuffer b) {
        try {
            b.reset();
            System.out.printf("%-38s -> OK, pos=%d%n", tag + ".reset()", b.position());
        } catch (Exception e) {
            System.out.printf("%-38s -> %s (pos stays %d)%n",
                    tag + ".reset()", e.getClass().getSimpleName(), b.position());
        }
    }

    public static void main(String[] args) {
        System.out.println("== A. no mark defined yet (mark == -1)");
        ByteBuffer a = ByteBuffer.allocate(8);
        a.put("ABC".getBytes());
        a.flip();
        p("flip()", a);
        tryReset("A", a);

        System.out.println();
        System.out.println("== B. mark() -> read 2 -> reset() goes back");
        ByteBuffer b = ByteBuffer.allocate(8);
        b.put("ABCDE".getBytes());
        b.flip();
        b.mark();
        System.out.println("   mark() at pos=" + b.position());
        b.get();
        b.get();
        p("get() x2", b);
        tryReset("B", b);

        System.out.println();
        System.out.println("== C. position(n) back below mark -> mark discarded");
        ByteBuffer c = ByteBuffer.allocate(8);
        c.put("ABCDE".getBytes());
        c.flip();
        c.position(3);
        c.mark();
        System.out.println("   position(3); mark() at pos=3");
        c.position(1);
        p("position(1)", c);
        tryReset("C", c);

        System.out.println();
        System.out.println("== D. flip() / rewind() / clear() each discard the mark");
        for (String op : new String[]{"flip", "rewind", "clear"}) {
            ByteBuffer d = ByteBuffer.allocate(8);
            d.put("ABCDE".getBytes());
            d.flip();
            d.position(2);
            d.mark();
            if (op.equals("flip")) d.flip();
            else if (op.equals("rewind")) d.rewind();
            else d.clear();
            p("  after " + op + "()", d);
            tryReset("  D." + op, d);
        }

        System.out.println();
        System.out.println("== E. real use: length-prefixed frame, truncated -> reset() and wait");
        ByteBuffer e = ByteBuffer.allocate(16);
        e.put((byte) 5).put("HELLO".getBytes());
        e.put((byte) 5).put("WOR".getBytes());
        e.flip();
        while (e.hasRemaining()) {
            e.mark();
            int len = e.get();
            if (e.remaining() < len) {
                System.out.println("   frame len=" + len + " but only " + e.remaining()
                        + " bytes left -> reset(), wait for more");
                e.reset();
                p("  after reset()", e);
                break;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) sb.append((char) e.get());
            System.out.println("   parsed frame = " + sb);
        }
    }
}
