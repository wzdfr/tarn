package com.dfrdev.tarn.util;


import com.dfrdev.tarn.core.TarnException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.Key;
import java.security.SecureRandom;
import java.security.Security;

/**
 * SecKit 提供对字符串的加解密.
 * <p>
 * Tarn 2.0+ 安全说明：
 * <ul>
 *     <li>新代码请使用 {@link #AESGCMEncrypt(String, String)} / {@link #AESGCMDecrypt(String, String)}
 *     （AES/GCM/NoPadding，认证加密，随机 IV）</li>
 *     <li>旧 AES/DES 方法保留以兼容存量数据，其中 DES 已被标记废弃，禁止用于新功能</li>
 *     <li>口令存储请使用 {@link #PBKDF2Encrypt(String, String, int)}（不要再用 MD5）</li>
 * </ul>
 *
 * @author DFR
 */
public class SecKit {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    /**
     * AES算法密钥生成器.
     *
     * @return 生成的密钥 它是一个32个字符的16进制字符串.
     */
    @SuppressWarnings("unused")
    public static String AESKey() {
        try {
            // Get the KeyGenerator
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128); // 192 and 256 bits may not be available
            // Generate the secret key specs.
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
            return byteArr2HexStr(raw);
        } catch (Exception e) {
            return "";
        }

    }

    /**
     * 使用AES算法解密字符串.
     * AES加密算法（美国国家标准局倡导的AES即将作为新标准取代DES）
     *
     * @param encrypted 要解密的字符串
     * @param rawKey    密钥字符串, 要求为一个32位(或64位，或128位)的16进制数的字符串,否则会出错
     * @return 解密之后的字符串
     * @see #AESDecrypt(String, String)
     * @deprecated 默认 ECB 模式不安全，仅保留用于解密存量数据；新代码请使用 {@link #AESGCMDecrypt(String, String)}
     */
    @Deprecated
    @SuppressWarnings({"JavaDoc", "unused"})
    public static String AESDecrypt(String encrypted, String rawKey) {
        byte[] tmp = hexStr2ByteArr(encrypted);
        byte[] key = hexStr2ByteArr(rawKey);
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            // 显式指定 ECB/PKCS5Padding（与旧 Cipher.getInstance("AES") 默认行为一致），消除隐式依赖
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] decrypted = cipher.doFinal(tmp);
            return new String(decrypted, UTF8);
        } catch (Exception e) {
            throw new TarnException("获取AES解密字符串出错。");
        }
    }

    /**
     * 使用AES算法加密字符串.
     * AES加密算法（美国国家标准局倡导的AES即将作为新标准取代DES）
     *
     * @param message 要加密的字符串.
     * @param rawKey  密钥字符串, 要求为一个32位(或64位，或128位)的16进制数的字符串,否则会出错
     * @return 加密之后的字符串
     * @see #AESDecrypt(String, String)
     * @deprecated 默认 ECB 模式不安全，仅保留用于兼容存量数据；新代码请使用 {@link #AESGCMEncrypt(String, String)}
     */
    @Deprecated
    @SuppressWarnings({"JavaDoc", "unused"})
    public static String AESEncrypt(String message, String rawKey) throws TarnException {
        byte[] key = hexStr2ByteArr(rawKey);
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(message.getBytes(UTF8));
            return byteArr2HexStr(encrypted);
        } catch (Exception e) {
            throw new TarnException("获取AES加密字符串出错。");
        }
    }

    /**
     * 使用 AES/GCM/NoPadding 认证加密（Tarn 2.0+ 推荐方式）。
     * 每次加密生成随机 12 字节 IV，输出格式：hex(IV) + hex(密文+认证标签)。
     *
     * @param message 明文
     * @param rawKey  16/24/32 字节密钥的 16 进制字符串（推荐 32 字节即 64 个 hex 字符）
     * @return hex(IV)+hex(cipher+tag) 格式密文
     */
    public static String AESGCMEncrypt(String message, String rawKey) throws TarnException {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(hexStr2ByteArr(rawKey), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(message.getBytes(UTF8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return byteArr2HexStr(out);
        } catch (Exception e) {
            throw new TarnException("AES-GCM加密出错。");
        }
    }

    /**
     * AES/GCM 认证解密，与 {@link #AESGCMEncrypt(String, String)} 配对。
     * 解密失败（密文被篡改/密钥错误）时抛出异常，具备完整性校验能力。
     */
    public static String AESGCMDecrypt(String encrypted, String rawKey) throws TarnException {
        try {
            byte[] all = hexStr2ByteArr(encrypted);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[all.length - GCM_IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(all, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(hexStr2ByteArr(rawKey), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), UTF8);
        } catch (Exception e) {
            throw new TarnException("AES-GCM解密出错（密文可能被篡改或密钥错误）。");
        }
    }

    /**
     * SHA-256 摘要（Tarn 2.0+），替代 MD5 用于一般性数据校验。
     */
    public static String SHA256Encrypt(String message) throws TarnException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return byteArr2HexStr(md.digest(message.getBytes(UTF8)));
        } catch (Exception e) {
            throw new TarnException("SHA-256摘要出错。");
        }
    }

    /**
     * PBKDF2 口令哈希（Tarn 2.0+，用于替代 MD5 存储口令）。
     *
     * @param password   口令明文
     * @param saltHex    随机盐（16 进制），建议 ≥16 字节，可由 {@link #randomSalt()} 生成
     * @param iterations 迭代次数，建议 ≥10000
     * @return 16 进制哈希；校验时用相同盐与迭代次数重算后经 {@link #safeEquals} 比较
     */
    public static String PBKDF2Encrypt(String password, String saltHex, int iterations) throws TarnException {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), hexStr2ByteArr(saltHex), iterations, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return byteArr2HexStr(factory.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            throw new TarnException("PBKDF2口令哈希出错。");
        }
    }

    /**
     * 生成 16 字节随机盐（16 进制字符串），配合 {@link #PBKDF2Encrypt(String, String, int)} 使用
     */
    public static String randomSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return byteArr2HexStr(salt);
    }

    /**
     * 常量时间字符串比较，防时序攻击，用于哈希值/令牌比对
     */
    public static boolean safeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(UTF8), b.getBytes(UTF8));
    }

    /**
     * 使用DES算法解密字符串.
     *
     * @param encrypted 要解密的字符串.
     * @param rawKey    密钥字符串, 可以为任意字符, 但最长不得超过8个字符(如最超过，后面的字符会被截掉).
     * @return 解密之后的字符串.
     * @see #DESEncrypt(String, String)
     * @deprecated DES 算法已不安全（密钥长度仅56位，可被暴力破解），仅保留用于兼容存量数据。
     *             新代码请使用 {@link #AESGCMDecrypt(String, String)}（AES/GCM 认证加密）。
     */
    @Deprecated
    @SuppressWarnings({"JavaDoc"})
    public static String DESDecrypt(String encrypted, String rawKey) throws TarnException {
        Security.addProvider(new com.sun.crypto.provider.SunJCE());
        byte[] arrBTmp = rawKey.getBytes();
        byte[] arrB = new byte[8]; // 创建一个空的8位字节数组（默认值为0）
        for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {// 将原始字节数组转换为8位
            arrB[i] = arrBTmp[i];
        }
        try {
            Key key = new SecretKeySpec(arrB, "DES");// 生成密钥
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(hexStr2ByteArr(encrypted)));
        } catch (Exception ex) {
            throw new TarnException("获取DES解密字符串出错。");
        }
    }

    /**
     * 使用DES算法加密字符串.
     *
     * @param message 要加密的字符串.
     * @param rawKey  密钥字符串,  可以为任意字符, 但最长不得超过8个字符(如最超过，后面的字符会被截掉).
     * @return 加密之后的字符串.
     * @see #DESDecrypt(String, String)
     * @deprecated DES 算法已不安全（密钥长度仅56位，可被暴力破解），仅保留用于兼容存量数据。
     *             新代码请使用 {@link #AESGCMEncrypt(String, String)}（AES/GCM 认证加密）。
     */
    @Deprecated
    @SuppressWarnings({"JavaDoc"})
    public static String DESEncrypt(String message, String rawKey) throws TarnException {
        Security.addProvider(new com.sun.crypto.provider.SunJCE());
        byte[] arrBTmp = rawKey.getBytes();
        byte[] arrB = new byte[8]; // 创建一个空的8位字节数组（默认值为0）
        for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {// 将原始字节数组转换为8位
            arrB[i] = arrBTmp[i];
        }
        try {
            Key key = new SecretKeySpec(arrB, "DES");// 生成密钥
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return byteArr2HexStr(cipher.doFinal(message.getBytes()));
        } catch (Exception ex) {
            throw new TarnException("获取DES加密字符串出错。");
        }
    }

    /**
     * 使用MD5算法加密字符串.
     *
     * @param message 要加密的字符串.
     * @param length  指定返回加密后字符串长度，其值必须是16或者32.
     * @return 加密之后的字符串
     * @throws dfr.tarn.core.TarnException 如果length参数的值不是16,也不是32，可能抛出此异常.
     * @deprecated MD5 算法已不安全（存在碰撞攻击和彩虹表攻击），不应用于口令存储或安全敏感场景。
     *             仅保留用于一般性数据校验（如文件完整性校验）。
     *             口令存储请使用 {@link #PBKDF2Encrypt(String, String, int)}，一般摘要请使用 {@link #SHA256Encrypt(String)}。
     */
    @Deprecated
    public static String MD5Encrypt(String message, int length) throws TarnException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(message.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (byte aB : b) {
                i = aB;
                if (i < 0) i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            String str32 = buf.toString().toUpperCase();
            if (length == 32) return str32;
            else if (length == 16) return str32.substring(8, 24);
            else throw new TarnException("对不起，第2个参数值必须是数字32或者16");
        } catch (Exception e) {
            throw new TarnException("获取MD5加密字符串出错。");
        }
    }


    /**
     * Turns array of bytes into string
     *
     * @param buf Array of bytes to convert to hex string
     * @return Generated hex string
     */
    private static String byteArr2HexStr(byte[] buf) {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;
        for (i = 0; i < buf.length; i++) {
            if (((int) buf[i] & 0xff) < 0x10) {
                strbuf.append("0");
            }
            strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
        }
        return strbuf.toString();
    }

    /**
     * 将表示16进制值的字符串转换为byte数组， 和public static String byteArr2HexStr(byte[] buf)互为可逆的转换过程
     *
     * @param src 需要转换的字符串
     * @return 转换后的byte数组
     */
    private static byte[] hexStr2ByteArr(String src) {
        if (src.length() < 1) {
            return null;
        }
        byte[] encrypted = new byte[src.length() / 2];
        for (int i = 0; i < src.length() / 2; i++) {
            int high = Integer.parseInt(src.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(src.substring(i * 2 + 1, i * 2 + 2), 16);

            encrypted[i] = (byte) (high * 16 + low);
        }
        return encrypted;
    }

    private SecKit() {
    }

}
