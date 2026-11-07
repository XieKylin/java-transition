package com.study.probe;

import java.nio.ByteBuffer;

/**
 * Day-16 probe: does put/get ever change limit?
 * Prints cap/pos/lim/rem after every single call. ASCII output only.
 */
public class BufProbe {

    static void p(String tag, ByteBuffer b) {
        System.out.printf("%-24s cap=%2d  pos=%2d  lim=%2d  rem=%2d%n",
                tag, b.capacity(), b.position(), b.limit(), b.remaining());
    }

    public static void main(String[] args) {
        System.out.println("== A. canonical: allocate -> put -> flip -> get -> clear");
        ByteBuffer a = ByteBuffer.allocate(16);
        p("allocate(16)", a); 
        a.put("ABCDE".getBytes());
        p("put 5 bytes", a);
        a.put("FG".getBytes());
        p("put 2 more bytes", a);
        a.flip();
        p("flip()", a);
        a.get();
        a.get();
        p("get() x2", a);
        a.clear();
        p("clear()", a);

        System.out.println();
        System.out.println("== B. counter-example 1: limit(n) caps the write");
        ByteBuffer b = ByteBuffer.allocate(16);
        b.limit(4);
        p("allocate(16).limit(4)", b);
        b.put("ABCD".getBytes());
        p("put 4 bytes (fits)", b);
        try {
            b.put((byte) 'E');
            p("put 5th byte", b);
        } catch (Exception e) {
            System.out.println("put 5th byte -> " + e.getClass().getSimpleName()
                    + "  [cap is still " + b.capacity() + ", lim is " + b.limit() + "]");
        }

        System.out.println();
        System.out.println("== C. counter-example 2: flip() uses current pos, not total put count");
        ByteBuffer c = ByteBuffer.allocate(16);
        c.put("ABCDEF".getBytes());
        p("put 6 bytes", c);
        c.position(3);
        p("position(3)", c);
        c.flip();
        p("flip()", c);
        System.out.println("=> lim is 3, NOT 6. flip() takes current pos, not cumulative writes.");

        System.out.println();
        System.out.println("== D. compact(): lim=capacity but pos != 0 (half-packet handling)");
        ByteBuffer d = ByteBuffer.allocate(8);
        d.put("ABCDEF".getBytes());
        d.flip();
        d.get();
        d.get();
        p("after reading A,B", d);
        d.compact();
        p("compact()", d);

        System.out.println();
        System.out.println("== E. rewind(): pos=0, lim unchanged");
        ByteBuffer e = ByteBuffer.allocate(8);
        e.put("XYZ".getBytes());
        e.flip();
        p("flip()", e);
        e.get();
        p("get() x1", e);
        e.rewind();
        p("rewind()", e);

        System.out.println();
        System.out.println("== F. put/get never touch limit");
        ByteBuffer f = ByteBuffer.allocate(8);
        p("allocate(8)", f);
        f.put("AB".getBytes());
        p("put 2", f);
        f.flip();
        p("flip()", f);
        f.get();
        p("get 1", f);
        System.out.println("=> in A~F, no put/get call changed lim. Only flip/clear/limit(n)/compact/rewind did.");
    }
}
