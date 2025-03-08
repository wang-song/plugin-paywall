package run.halo.starter;

import cn.hutool.http.HttpUtil;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static cn.hutool.crypto.digest.DigestAlgorithm.MD5;

@Component
public class MyUtils {


    public static Mono<String> createOrder(String payId,double price,ReactiveExtensionClient client){


        return client.fetch(MqSetting.class,"mqsetting")
            .flatMap(setting -> {

                String serverUrl = setting.getSpec().getServerUrl();
                String key = setting.getSpec().getKey();
                boolean isHttps = setting.getSpec().isHttps();
                if (serverUrl == null) {
                    return Mono.error(new RuntimeException("创建订单失败：无法获取必要设置"));
                }

                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("payId", payId);
                    data.put("price", price);
                    data.put("type", 1);
                    data.put("sign", getMD5Hash(payId + 1 + price + key));
                    System.out.println("data:"+data);
                    String result = HttpUtil.post(getCreateOrderUrl(serverUrl,isHttps), data);
                    System.out.println("result:"+result);

                    return Mono.just(result);
                } catch (Exception e) {
                    return Mono.error(new RuntimeException("创建订单失败：" + e.getMessage()));
                }

            })
            .onErrorResume(e -> {
                return Mono.just("创建订单失败");
            });

    }

    private static String getCreateOrderUrl(String serverUrl,boolean isHttps) {
        if(isHttps){
            serverUrl = "https://"+serverUrl + "/createOrder";
        }else{
            serverUrl = "http://"+serverUrl + "/createOrder";
        }
        return serverUrl;
    }

    public static String getMD5Hash(String input) {
        try {
            // 获取MessageDigest实例，指定算法为MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 将输入字符串转换为字节数组并进行哈希计算
            byte[] messageDigest = md.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

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
