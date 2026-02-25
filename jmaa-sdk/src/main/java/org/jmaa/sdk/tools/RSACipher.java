package org.jmaa.sdk.tools;

import org.jmaa.sdk.exceptions.PlatformException;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author Eric Liang
 */
@SuppressWarnings("AlibabaClassNamingShouldBeCamel")
public class RSACipher {
    static final String KEY_ALGORITHM = "RSA";

    /**
     * 私钥解密
     * @param data 待解密数据
     * @param key  私钥
     * @return byte[] 解密数据
     */
    public static byte[] decryptByPrivateKey(byte[] data, byte[] key)
    {
        try
        {
            //取得私钥
            PKCS8EncodedKeySpec pkcs = new PKCS8EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            //生成私钥
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs);
            //对数据解密
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        }
        catch(Exception e)
        {
            throw new PlatformException("解密失败", e);
        }
    }

    /**
     * 公钥解密
     * @param data
     * @param key
     * @return
     */
    public static byte[] decryptByPublicKey(byte[] data, byte[] key)
    {
        try
        {
            //取得公钥
            X509EncodedKeySpec pkcs = new X509EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            //生成公钥
            PublicKey publicKey = keyFactory.generatePublic(pkcs);
            //对数据解密
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        }
        catch(Exception e)
        {
            throw new PlatformException("解密失败", e);
        }
    }

    /**
     * 公钥加密
     * @param data
     * @param key
     * @return
     */
    public static byte[] encryptByPublicKey(byte[] data, byte[] key)
    {
        try
        {
            //取得公钥
            X509EncodedKeySpec pkcs = new X509EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            //生成公钥
            PublicKey publicKey = keyFactory.generatePublic(pkcs);
            //对数据加密
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        }
        catch(Exception e)
        {
            throw new PlatformException("加密失败", e);
        }
    }

    /**
     * 私钥加密
     * @param data
     * @param key
     * @return
     */
    public static byte[] encryptByPrivateKey(byte[] data, byte[] key)
    {
        try
        {
            //取得私钥
            PKCS8EncodedKeySpec pkcs = new PKCS8EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            //生成公钥
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs);
            //对数据加密
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        }
        catch(Exception e)
        {
            throw new PlatformException("加密失败", e);
        }
    }
}
