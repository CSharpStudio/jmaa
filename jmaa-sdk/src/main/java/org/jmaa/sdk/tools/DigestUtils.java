/*
 *      Copyright (c) 2018-2028, DreamLu All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: DreamLu 卢春梦 (596392912@qq.com)
 */

package org.jmaa.sdk.tools;

import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.util.SecurityCode;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * 加密相关工具类
 *
 * @author eric
 */
public class DigestUtils extends org.springframework.util.DigestUtils {
    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param data Data to digest
     * @return MD5 digest as a hex string
     */
    public static String md5Hex(final String data) {
        return org.springframework.util.DigestUtils.md5DigestAsHex(data.getBytes(StandardCharsets.UTF_8));
    }

    public static class DES {

        static final byte[] BYTES_KEY = {64, -108, 71, -9, -20, -113, 104, -119};

        /**
         * 使用DES算法编码
         *
         * @param data
         * @return
         */
        public static String encode(String data) {
            return encode(data, BYTES_KEY);
        }

        /**
         * 使用DES算法编码
         *
         * @param
         * @return
         */
        public static String encode(String data, byte[] bytesKey) {
            try {
                DESKeySpec desKeySpec = new DESKeySpec(bytesKey);
                SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
                Key convertSecretKey = factory.generateSecret(desKeySpec);
                Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, convertSecretKey);
                byte[] bytes = data.getBytes();
                int len = Math.max(30, bytes.length + 4);
                byte[] source = new byte[len];
                int idx = 0;
                for (byte b : ObjectUtils.toByteArray(bytes.length)) {
                    source[idx++] = b;
                }
                for (byte b : bytes) {
                    source[idx++] = b;
                }
                while (idx < len) {
                    source[idx++] = (byte) idx;
                }
                byte[] result = cipher.doFinal(source);
                return StringUtils.encodeHexString(result);
            } catch (Exception e) {
                return "error";
            }
        }

        /**
         * 使用DES算法解码
         *
         * @param data
         * @return
         */
        public static String decode(String data) {
            return decode(data, BYTES_KEY);
        }

        /**
         * 使用DES算法解码
         *
         * @param data
         * @return
         */
        public static String decode(String data, byte[] bytesKey) {
            try {
                DESKeySpec desKeySpec = new DESKeySpec(bytesKey);
                SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
                Key convertSecretKey = factory.generateSecret(desKeySpec);
                Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, convertSecretKey);
                byte[] hex = StringUtils.decodeHex(data);
                byte[] source = cipher.doFinal(hex);
                int len = ObjectUtils.toInt(new byte[]{source[0], source[1], source[2], source[3]});
                byte[] result = new byte[len];
                for (int i = 0; i < len; i++) {
                    result[i] = source[i + 4];
                }
                return new String(result);
            } catch (Exception e) {
                throw new AccessException("密钥无效", e, SecurityCode.TOKEN_ERROR);
            }
        }
    }
}
