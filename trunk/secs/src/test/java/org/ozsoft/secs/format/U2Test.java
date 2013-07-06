package org.ozsoft.secs.format;

import org.junit.Assert;
import org.junit.Test;

public class U2Test {
    
    @Test
    public void test() {
        U2 u2 = new U2();
        u2.addValue(0);
        u2.addValue(1);
        u2.addValue(255);
        u2.addValue(256);
        u2.addValue(257);
        u2.addValue(65535);
        Assert.assertEquals(6, u2.length());
        Assert.assertEquals("U2:6 {0 1 255 256 257 65535}", u2.toSml());
        TestUtils.assertEquals(new byte[] {(byte) 0xa9, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, (byte) 0xff, 0x01, 0x00, 0x01, 0x01, (byte) 0xff, (byte) 0xff}, u2.toByteArray());
    }
    
}
