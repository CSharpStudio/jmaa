package org.jmaa.sdk.tools;

import org.jmaa.sdk.exceptions.ValueException;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collection;

/**
 * 字符串工具
 *
 * @author Eric Liang
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {
    /**
     * 拼接字符串
     * <pre>
     *     join(null) -> null
     *     join(Arrays.asList("a", "b")) -> a,b
     * </pre>
     *
     * @param items
     * @return
     */
    public static String join(Collection<String> items) {
        return join(items, ",");
    }

    /**
     * 拼接字符串
     * <pre>
     *     join(null) -> null
     *     join("a", "b") -> a,b
     * </pre>
     *
     * @param elements
     * @return
     */
    public static <T> String join(T... elements) {
        return join(elements, ",");
    }

    /**
     *
     * @param message
     * @param arguments
     * @return
     */
    public static String format(@Nullable String message, @Nullable Object... arguments) {
        if (isNotEmpty(message)) {
            if (message.indexOf("%s") > -1) {
                return String.format(message, arguments);
            }
            if (message.indexOf("{}") > -1) {
                return MessageFormatter.arrayFormat(message, arguments, null).getMessage();
            }
            return MessageFormat.format(message, arguments);
        }
        return "";
    }

    /**
     * 是否包含大写
     *
     * @param cs
     * @return
     */
    public static boolean hasUpperCase(String cs) {
        if (isEmpty(cs)) {
            return false;
        } else {
            int sz = cs.length();
            for (int i = 0; i < sz; ++i) {
                if (Character.isUpperCase(cs.charAt(i))) {
                    return true;
                }
            }
            return false;
        }
    }


    public static final Charset DEFAULT_CHARSET;
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";
    private static final char[] DIGITS_LOWER;
    private static final char[] DIGITS_UPPER;

    public static byte[] decodeHex(char[] data) {
        byte[] out = new byte[data.length >> 1];
        decodeHex(data, out, 0);
        return out;
    }

    public static int decodeHex(char[] data, byte[] out, int outOffset) {
        int len = data.length;
        if ((len & 1) != 0) {
            throw new ValueException("Odd number of characters.");
        } else {
            int outLen = len >> 1;
            if (out.length - outOffset < outLen) {
                throw new ValueException("Output array is not large enough to accommodate decoded data.");
            } else {
                int i = outOffset;

                for(int j = 0; j < len; ++i) {
                    int f = toDigit(data[j], j) << 4;
                    ++j;
                    f |= toDigit(data[j], j);
                    ++j;
                    out[i] = (byte)(f & 255);
                }

                return outLen;
            }
        }
    }

    public static byte[] decodeHex(String data) {
        return decodeHex(data.toCharArray());
    }

    public static char[] encodeHex(byte[] data) {
        return encodeHex(data, true);
    }

    public static char[] encodeHex(byte[] data, boolean toLowerCase) {
        return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    protected static char[] encodeHex(byte[] data, char[] toDigits) {
        int l = data.length;
        char[] out = new char[l << 1];
        encodeHex(data, 0, data.length, toDigits, out, 0);
        return out;
    }

    public static char[] encodeHex(byte[] data, int dataOffset, int dataLen, boolean toLowerCase) {
        char[] out = new char[dataLen << 1];
        encodeHex(data, dataOffset, dataLen, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER, out, 0);
        return out;
    }

    public static void encodeHex(byte[] data, int dataOffset, int dataLen, boolean toLowerCase, char[] out, int outOffset) {
        encodeHex(data, dataOffset, dataLen, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER, out, outOffset);
    }

    private static void encodeHex(byte[] data, int dataOffset, int dataLen, char[] toDigits, char[] out, int outOffset) {
        int i = dataOffset;

        for(int j = outOffset; i < dataOffset + dataLen; ++i) {
            out[j++] = toDigits[(240 & data[i]) >>> 4];
            out[j++] = toDigits[15 & data[i]];
        }

    }

    public static char[] encodeHex(ByteBuffer data) {
        return encodeHex(data, true);
    }

    public static char[] encodeHex(ByteBuffer data, boolean toLowerCase) {
        return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    protected static char[] encodeHex(ByteBuffer byteBuffer, char[] toDigits) {
        return encodeHex(toByteArray(byteBuffer), toDigits);
    }

    public static String encodeHexString(byte[] data) {
        return new String(encodeHex(data));
    }

    public static String encodeHexString(byte[] data, boolean toLowerCase) {
        return new String(encodeHex(data, toLowerCase));
    }

    public static String encodeHexString(ByteBuffer data) {
        return new String(encodeHex(data));
    }

    public static String encodeHexString(ByteBuffer data, boolean toLowerCase) {
        return new String(encodeHex(data, toLowerCase));
    }

    private static byte[] toByteArray(ByteBuffer byteBuffer) {
        int remaining = byteBuffer.remaining();
        byte[] byteArray;
        if (byteBuffer.hasArray()) {
            byteArray = byteBuffer.array();
            if (remaining == byteArray.length) {
                byteBuffer.position(remaining);
                return byteArray;
            }
        }

        byteArray = new byte[remaining];
        byteBuffer.get(byteArray);
        return byteArray;
    }

    protected static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new ValueException("Illegal hexadecimal character " + ch + " at index " + index);
        } else {
            return digit;
        }
    }

    static {
        DEFAULT_CHARSET = StandardCharsets.UTF_8;
        DIGITS_LOWER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        DIGITS_UPPER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    }
}
