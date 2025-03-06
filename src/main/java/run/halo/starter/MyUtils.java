package run.halo.starter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class MyUtils {


    /**
     * 根据字符串生成确定性的UUID
     * 相同的输入字符串将始终生成相同的UUID
     *
     * @param input 输入字符串
     * @return UUID字符串
     */
    public static String generateDeterministicUUID(String input) {
        try {
            // 使用 MD5 生成确定性的哈希值
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // 将前16个字节转换为UUID格式
            ByteBuffer bb = ByteBuffer.wrap(hashBytes);
            long high = bb.getLong();
            long low = bb.getLong();

            UUID uuid = new UUID(high, low);
            return uuid.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果 MD5 不可用，使用简单的哈希方法
            return String.format("%032x", input.hashCode());
        }
    }
}
