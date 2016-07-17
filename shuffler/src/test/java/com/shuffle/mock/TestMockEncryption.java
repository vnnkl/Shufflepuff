package com.shuffle.mock;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Daniel Krawisz on 7/16/16.
 */
public class TestMockEncryption {

    @Test
    public void testEncryption() {

        Assert.assertTrue(
                new MockEncryptionKey(1).encrypt("abcd").equals("abcd~encrypt[1]"));
        Assert.assertTrue(
                new MockEncryptionKey(2).encrypt("abcd").equals("abcd~encrypt[2]"));
        Assert.assertTrue(
                new MockEncryptionKey(1).encrypt("abcd~decrypt[1]").equals("abcd"));
        Assert.assertTrue(
                new MockEncryptionKey(2).encrypt("abcd~decrypt[2]").equals("abcd"));
        Assert.assertTrue(
                new MockEncryptionKey(2).encrypt("abcd~decrypt[1]")
                        .equals("abcd~decrypt[1]~encrypt[2]"));
        Assert.assertTrue(
                new MockEncryptionKey(1).encrypt("abcd~decrypt[2]")
                        .equals("abcd~decrypt[2]~encrypt[1]"));
    }

    @Test
    public void testDecryption() {
        System.out.println(new MockEncryptionKey(2).encrypt("abcd~decrypt[1]"));

        Assert.assertTrue(
                new MockDecryptionKey(1).decrypt("abcd").equals("abcd~decrypt[1]"));
        Assert.assertTrue(
                new MockDecryptionKey(2).decrypt("abcd").equals("abcd~decrypt[2]"));
        Assert.assertTrue(
                new MockDecryptionKey(1).decrypt("abcd~encrypt[1]").equals("abcd"));
        Assert.assertTrue(
                new MockDecryptionKey(2).decrypt("abcd~encrypt[2]").equals("abcd"));
        Assert.assertTrue(
                new MockDecryptionKey(2).decrypt("abcd~encrypt[1]")
                        .equals("abcd~encrypt[1]~decrypt[2]"));
        Assert.assertTrue(
                new MockDecryptionKey(1).decrypt("abcd~decrypt[2]")
                        .equals("abcd~encrypt[2]~decrypt[1]"));
    }
}
