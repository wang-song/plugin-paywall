package run.halo.starter;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.HashMap;

@RestController
@RequestMapping("/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-paywall")
@RequiredArgsConstructor
public class PaywallEndpoint {

    private final ReactiveExtensionClient client;

    private String generatePreview(String content) {
        if (content == null || content.length() <= 100) {
            return content;
        }
        return content.substring(0, 5) + "...";
    }

    @GetMapping("/content/{contentId}")
    public Mono<Map<String, String>> getContent(@PathVariable String contentId) {
        // 首先获取内容信息
        return client.fetch(PaywallContent.class, contentId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在")))
            .flatMap(content -> {
                if (content.getStatus() == null || !"Active".equals(content.getStatus().getPhase())) {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "内容已过期"));
                }

                // 检查是否已购买
                Predicate<PaymentRecord> predicate = record -> {
                    Map<String, String> labels = record.getMetadata().getLabels();
                    return labels != null && 
                           contentId.equals(labels.get("content-id")) &&
                           "SUCCESS".equals(labels.get("status"));
                };

                return client.list(PaymentRecord.class, predicate, null)
                    .collectList()
                    .map(records -> {
                        Map<String, String> response = new HashMap<>();
                        String fullContent = content.getSpec().getContent();
                        
                        if (!records.isEmpty()) {
                            // 已购买，返回完整内容
                            response.put("content", fullContent);
                            response.put("isPaid", "true");
                        } else {
                            // 未购买，返回预览内容和购买信息
                            response.put("content", generatePreview(fullContent));
                            response.put("isPaid", "false");
                            response.put("price", String.valueOf(content.getSpec().getPrice()));
                            response.put("message", "此内容需要付费才能查看完整内容");
                        }
                        return response;
                    });
            });
    }

    @PostMapping("/purchase/{contentId}")
    public Mono<Map<String, String>> purchase(@PathVariable String contentId) {
        return client.fetch(PaywallContent.class, contentId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在")))
            .flatMap(content -> {
                if (content.getStatus() == null || !"Active".equals(content.getStatus().getPhase())) {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "内容已过期"));
                }

                Predicate<PaymentRecord> predicate = record -> {
                    Map<String, String> labels = record.getMetadata().getLabels();
                    return labels != null && 
                           contentId.equals(labels.get("content-id")) &&
                           "SUCCESS".equals(labels.get("status"));
                };

                return client.list(PaymentRecord.class, predicate, null)
                    .collectList()
                    .flatMap(records -> {
                        if (!records.isEmpty()) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "此内容已被购买"));
                        }
                        
                        String orderId = UUID.randomUUID().toString();
                        Map<String, String> response = new HashMap<>();
                        response.put("orderId", orderId);
                        response.put("qrCodeUrl", "https://example.com/pay/" + orderId);
                        response.put("price", String.valueOf(content.getSpec().getPrice()));
                        return Mono.just(response);
                    });
            });
    }

    @GetMapping("/purchase/status/{orderId}")
    public Mono<Map<String, String>> checkPurchaseStatus(@PathVariable String orderId) {
        return Mono.just(Map.of(
            "status", "PENDING",  // 可能的状态：PENDING, SUCCESS, FAILED
            "message", "等待支付"
        ));
    }
} 