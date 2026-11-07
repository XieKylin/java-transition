package com.study.probe;

import java.nio.ByteBuffer;

/**
 * Day-16 probe #2: can you put() after flip() before clear()?
 * Answer is measured, not asserted. ASCII output only.
 */
public class FlipPutProbe {

    static void p(String tag, ByteBuffer b) {
        System.out.printf("%-32s cap=%2d pos=%2d lim=%2d rem=%2d%n",
                tag, b.capacity(), b.position(), b.limit(), b.remaining());
    }

    /** absolute get, does NOT move position */
    static String dump(ByteBuffer b, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append((char) b.get(i));
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println("== A. put AFTER flip(), no clear() in between");
        ByteBuffer a = ByteBuffer.allocate(16);
        a.put("HELLO".getBytes());
        p("put HELLO", a);
        a.flip();
        p("flip() -> read mode", a);
        System.out.println("   content right now        = " + dump(a, 5));

        a.put("XY".getBytes());
        p("put XY while in read mode", a);
        System.out.println("   content after that put   = " + dump(a, 5) + "   <-- H,E overwritten");

        StringBuilder rest = new StringBuilder();
        while (a.hasRemaining()) rest.append((char) a.get());
        System.out.println("   what get() will return   = " + rest + "   <-- H,E silently gone");
        System.out.println("   => no exception thrown, no warning, data corrupted");

        System.out.println();
        System.out.println("== B. put in read mode throws ONLY when remaining runs out");
        ByteBuffer b = ByteBuffer.allocate(8);
        b.put("ABC".getBytes());
        b.flip();
        p("flip()", b);
        b.put("123".getBytes());
        p("put 3 bytes (eats remaining)", b);
        try {
            b.put((byte) 'X');
            p("put 4th byte", b);
        } catch (Exception e) {
            System.out.println("put 4th byte -> " + e.getClass().getSimpleName()
                    + "   (remaining was 0)");
        }

        System.out.println();
        System.out.println("== C. the only state-like restriction is read-only, not mode");
        ByteBuffer c = ByteBuffer.allocate(8);
        c.put("AB".getBytes());
        ByteBuffer ro = c.asReadOnlyBuffer();
        p("asReadOnlyBuffer()", ro);
        try {
            ro.put((byte) 'X');
            p("ro.put()", ro);
        } catch (Exception e) {
            System.out.println("ro.put() -> " + e.getClass().getSimpleName());
        }

        System.out.println();
        System.out.println("=> rule for put(): position + n <= limit, and not read-only. No mode check anywhere.");
    }
}
