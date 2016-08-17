package com.shuffle.mock;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.protocol.FormatException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Daniel Krawisz on 8/14/16.
 */
public class TestMockSigningKey {

    public void catchExceptionTest(String str) {

        try {
            new MockSigningKey(str);
            Assert.fail();
        } catch (FormatException e) {

        }
    }

    @Test
    public void testMockSigningKey() throws FormatException {
        catchExceptionTest("");
        catchExceptionTest("sk[]");
        catchExceptionTest("sk[abc]");

        for (int i = 0; i < 11; i ++) {
            SigningKey s = new MockSigningKey(i);
            Assert.assertTrue(s.equals(new MockSigningKey(s.toString())));
        }
    }
}
