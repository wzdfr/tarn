package com.dfrdev.tarn.util;

import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.util.crypto.SecKit;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * SecKit 安全工具类单元测试。
 * 覆盖 AES-GCM 加解密、SHA256、PBKDF2 等推荐算法。
 */
public class SecKitTest {

    // AES-128 需要 16 字节 = 32 个 hex 字符
    private static final String KEY = "0123456789abcdef0123456789abcdef";
    private static final String MESSAGE = "Hello, Tarn Framework!";

    // ==================== AES-GCM ====================

    @Test
    public void testAESGCMEncryptDecrypt() throws TarnException {
        String encrypted = SecKit.AESGCMEncrypt(MESSAGE, KEY);
        assertNotNull("加密结果不应为 null", encrypted);
        assertNotEquals("加密结果不应等于原文", MESSAGE, encrypted);

        String decrypted = SecKit.AESGCMDecrypt(encrypted, KEY);
        assertEquals("解密结果应等于原文", MESSAGE, decrypted);
    }

    @Test
    public void testAESGCMDifferentCiphertextEachTime() throws TarnException {
        // AES-GCM 使用随机 IV，同一明文每次加密结果应不同
        String enc1 = SecKit.AESGCMEncrypt(MESSAGE, KEY);
        String enc2 = SecKit.AESGCMEncrypt(MESSAGE, KEY);
        assertNotEquals("同一明文两次加密结果应不同（随机 IV）", enc1, enc2);
    }

    @Test(expected = TarnException.class)
    public void testAESGCMDecryptWithWrongKey() throws TarnException {
        String encrypted = SecKit.AESGCMEncrypt(MESSAGE, KEY);
        // 使用不同的合法 hex key
        SecKit.AESGCMDecrypt(encrypted, "abcdef0123456789abcdef0123456789");
    }

    // ==================== SHA256 ====================

    @Test
    public void testSHA256Encrypt() throws TarnException {
        String hash = SecKit.SHA256Encrypt(MESSAGE);
        assertNotNull("SHA256 哈希不应为 null", hash);
        assertTrue("SHA256 哈希长度应为 64", hash.length() == 64);

        // 同一输入应产生相同哈希
        String hash2 = SecKit.SHA256Encrypt(MESSAGE);
        assertEquals("同一输入 SHA256 应一致", hash, hash2);
    }

    // ==================== PBKDF2 ====================

    @Test
    public void testPBKDF2Encrypt() throws TarnException {
        // salt 需要是 hex 字符串（16 字节 = 32 hex chars）
        String salt = "abcdef0123456789abcdef0123456789";
        String hash = SecKit.PBKDF2Encrypt("password123", salt, 10000);
        assertNotNull("PBKDF2 哈希不应为 null", hash);
        assertTrue("PBKDF2 哈希长度应大于 0", hash.length() > 0);

        // 同一输入应产生相同哈希
        String hash2 = SecKit.PBKDF2Encrypt("password123", salt, 10000);
        assertEquals("同一输入 PBKDF2 应一致", hash, hash2);
    }

    @Test
    public void testPBKDF2DifferentSalts() throws TarnException {
        String hash1 = SecKit.PBKDF2Encrypt("password", "11111111111111111111111111111111", 10000);
        String hash2 = SecKit.PBKDF2Encrypt("password", "22222222222222222222222222222222", 10000);
        assertNotEquals("不同盐值 PBKDF2 应不同", hash1, hash2);
    }
}
