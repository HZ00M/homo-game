package com.homo.test.mock;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@UtilityClass
@Slf4j
public class TestSignatureUtil {
    private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();

    //将非ASCII字符的数据转换成ASCII字符
    public static String contentMd5(byte[] content) throws NoSuchAlgorithmException {
        log.debug("contentMd5 {}",new String(content, StandardCharsets.UTF_8));
        return BASE_64_ENCODER.encodeToString(md5Bytes(content));
    }

    //使用md5生成数字签名128位（16字节）的散列值，16进制字符占4位，将md5的128转换成16进制可以得到固定长度32的16进制字符串
    public static byte[] md5Bytes(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(content);
        return messageDigest.digest();
    }

    //将byte数组转换成字符串
    public static String hex(byte[] bts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bts.length; i++) {
            sb.append(Integer.toHexString((bts[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String sign(Map<String, String> params, String secret) throws NoSuchAlgorithmException {
        String secretOrigin = getSignatureString(params) + secret;
        return hex(md5Bytes(secretOrigin.getBytes(StandardCharsets.UTF_8)));
    }


    private static String getSignatureString(Map<String, String> param) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
