package run.halo.starter;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ApiVersion;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.List;

/**
 * 付费内容 API 端点
 * 提供付费内容的访问、购买和支付状态查询功能
 */
@ApiVersion("plugin-paywall.halo.run/v1alpha1")
@RestController
@RequestMapping("/paywall")
@RequiredArgsConstructor
public class PaywallEndpoint {

    /**
     * Kubernetes 资源操作客户端
     * 用于操作 PaywallContent 和 PaymentRecord 资源
     */
    private final ReactiveExtensionClient client;

    /**
     * 生成内容预览
     * 如果内容长度超过100个字符，则截取前100个字符并添加省略号
     *
     * @param content 原始内容
     * @return 预览内容
     */
    private String generatePreview(String content) {
        if (content == null || content.length() <= 100) {

            return content;
        }
        return content.substring(0, 100) + "...";
    }

    /**
     * 获取内容接口
     * 根据内容ID返回对应的内容，如果未购买则返回预览内容
     *
     * @param contentId 内容ID
     * @return 包含内容信息的Map，包括：
     *         - content: 内容文本（完整内容或预览内容）
     *         - isPaid: 是否已购买（true/false）
     *         - price: 价格（未购买时）
     *         - message: 提示信息（未购买时）
     */
    @GetMapping("/content/{contentId}")
    public Mono<Map<String, String>> getContent(@PathVariable("contentId") String contentId) {
        // 首先获取内容信息
        return client.fetch(PaymentRecord.class, contentId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在")))
            .flatMap(content -> {

                String payStatus = content.getSpec().getPayStatus();

                Map<String, String> response = new HashMap<>();

                if (PayStatus.PENDING.name().equals(payStatus)) {
                    //未支付
                    // response.put("content", generatePreview(fullContent));
                    response.put("payStatus", "PENDING");
                    response.put("price", String.valueOf(content.getSpec().getPrice()));
                    response.put("message", "此内容需要付费才能查看完整内容");


                }else if (PayStatus.SUCCESS.name().equals(payStatus)) {
                    //已支付
                    response.put("content", content.getSpec().getContent());
                    response.put("payStatus", "SUCCESS");


                }else if (PayStatus.FAILED.name().equals(payStatus)) {
                    //支付失败
                    response.put("payStatus", "FAILED");
                    response.put("price", String.valueOf(content.getSpec().getPrice()));
                    response.put("message", "支付失败");

                }
                return Mono.just(response);


            });
    }

    /**
     * 购买内容接口
     * 创建新的支付订单，返回支付二维码URL
     *
     * @param clientAndContentString 要购买的内容ID
     * @return 包含订单信息的Map，包括：
     * - orderId: 订单ID
     * - qrCodeUrl: 支付二维码URL
     * - price: 支付金额
     */
    @RequestMapping ("/purchase/{clientAndContentString}")
    public Mono<PaymentRecordDto> purchase(@PathVariable("clientAndContentString") String clientAndContentString) {

        String base64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJEAAACGCAYAAADdC2gAAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAATcSURBVHhe7dyxbxtlGMfxx/bVSpQu0FCKGilthgxVBitMHQoDgQWUuSyoqlRlAXWqGFCXRv0DUKcICQHqwsCShYEwoO5VhgqhCqVQFVQGGJCCQurmGJIjl6fn8zm/N+eL7/uRTqrf9y52+377OrEjN+I4jq2PjY0Nm5mZ8cOAmZk1/QAwKCKCjIggIyLIiAgyIoKMiCAjIsiICDIigoyIICMiyIgIMiKCrFH0V0EmJyf9MGDGToQQSt2J1tfX/dDAOp2OH8KQsRNBRkSQBY1odXXVD6EGgkVEQPUVJCICqjc5IgKCHNHi4qIfQs3IERkh1V6QiIyQai1YREZItRU0ItQTEUFW6huwGE3sRJAV2okePfrF4njHDwNmRSN6/PgPO336JT8MmPF0hhCICDIigoyIICMiyIgIMiKCLOjrRPHTh2ZrK7bz5IHZ866fRhlakTWn5swWlqxxZtbPHolgEcVPH9rOFx+ZdbtmDT+LUsVmFkXWvHKnlJDCPZ2trRBQVTRsdy3WVvzMkQgW0c6TBwRUJY29NSlBsIj4HqiCSlqTcBGhtogIMiKCjIggq05EZy9a1Hn74DE3b42z89Z62Z+MKgn2YuPz22/5oYGcuPqttc+fPDi4dd+2fzxl7dk/bfuz6/bsr4PTeSaW79nmzUt+uJB+12bN+7Hkth9P5rzkXM9fO6jWJ9/7oeCqsxPlOTlv7Wuf2omCO1KyGBPL93KPrGtCy4pj8+al/+PI+nN67DgYYkQXrX31ro1/eNfG3//AT+779Qfrrn9n3Z9/t8aUn3xR8j+/yOGl4/Kh+RDSkvtU5d1HlQ0xojesdX7amq9OW/OV1/zkrrF5a7972aILb1p04R1rTfsTDkovpt910kcv6bh6hXYYob5OVQ0xooLa7f0j8pMH+cXyQfj5hLKT5EXp402fq9xn1VQ/ItHm3vckeYttbsGzbmfxIfhr0vFmRezPP65GOiK/QL0WK2uh/e0sfq7INZZ6HFnn+rD836GKRjairBiyFi3Ra/GOYgF7PQb/OPMeb5WMbERZMfSLIr1o6gJuZvxoP6pGMqKJ1I/5lhHHYRz2uqJ86P2Cr5LqRNT38yK2zf72Yy9KAkokO4If7yfEAvrdKO9rJqH74zioRkSnXrfmT7ds65tb9m+v4+sbtlXgFXz/D5/elfIW0UvOT3+9Qa4vIm+36TVeRUN87+xjG19+L7/irfu2ffu6PfPjOfw/flZUaX7eMnaztKy5rLG05D59kMntXtdnXTeoMt47G2JEl23sxjVrjfnxffFvX9o/n3/lhzGAEY8IZSgjotxnE6AIIoKMiCAjIsiICDIigoyIIAsXUavPrx2ifCWtSbCImlNzu5+Lg2qI99akBMEisoUlsygipCrY+5ArW1jyM0ciWESNM7PWvHLHmuc6pW2jyNCKrHmuU9qnpFnI985QX8F2ItQXEUFGRJAREWREBBkRQUZEkBV+nQjopVBEQB6eziAjIsiICDIigoyIICMiyIgIMiKCjIggIyLIiAgyIoKMiCAjIsiICDIigoyIICMiyIgIMiKCjIggIyLIiAgyIoKMiCAjIsiICDIigoyIICMiyIgIMiKCjIggIyLIiAgyIoKMiCAjIsiICDIigoyIICMiyIgIMiKCjIggIyLIiAgy/wDLvjTpXl+rHAAAAABJRU5ErkJggg==";
        // 检查内容是否存在
        JSONObject jsonObject = JSONUtil.parseObj(clientAndContentString);
        String contentId = jsonObject.getStr("contentId", "");
        String clientId = jsonObject.getStr("clientId", "");

        //利用contentId和clientId生成订单名，唯一标识一个订单，一定是这个用户、这个内容的订单
        String name = MyUtils.generateDeterministicUUID(contentId + ":" + clientId);


        System.out.println("开始创建订单contentId: " + contentId);


        return client.fetch(PaymentRecord.class, name)
            .switchIfEmpty(Mono.defer(() -> {
                System.out.println("没有找到订单，开始创建订单");
                PaymentRecord paymentRecord = new PaymentRecord();
                paymentRecord.setMetadata(new Metadata());
                paymentRecord.getMetadata().setName(name);
                PaymentRecord.PaymentRecordSpec spec = new PaymentRecord.PaymentRecordSpec();
                spec.setContentId(contentId);
                spec.setClientId(clientId);
                spec.setPayStatus(PayStatus.PENDING.name());
                spec.setCreateTime(System.currentTimeMillis());
                spec.setExpireTime(System.currentTimeMillis() + 1000 * 60 * 5);

                // 查询contentRecord，获取价格和内容
                return client.get(ContentRecord.class, contentId)
                    .onErrorResume(e -> {
                        System.err.println("查询 ContentRecord 失败: " + e.getMessage());
                        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "查询内容失败"));
                    })
                    .flatMap(contentRecord -> {
                        spec.setPrice(contentRecord.getSpec().getPrice());
                        spec.setContent(contentRecord.getSpec().getContent());
                        spec.setPreviewContent(contentRecord.getSpec().getPreviewContent());
                        paymentRecord.setSpec(spec);
                        System.out.println("开始创建订单，生成二维码");

                        // MyUtils.createOrder(name, contentRecord.getSpec().getPrice(), client);
                        // 将MyUtils.createOrder的结果作为Mono处理并集成到反应式链中 订单创建成功后再创建 PaymentRecord 资源
                        return MyUtils.createOrder(name, contentRecord.getSpec().getPrice(), client)
                            .flatMap(result -> {
                                System.out.println("订单创建结果: " + result);
                                if(JSONUtil.parseObj(result).getInt("code",0) == 1){
                                    JSONObject data = JSONUtil.parseObj(result).getJSONObject("data");

                                    paymentRecord.getSpec().setQrCodeUrl(data.getStr("payUrl"));
                                    paymentRecord.getSpec().setOrderId(data.getStr("orderId"));

                                    // paymentRecord.setSpec(spec);
                                    System.out.println("订单创建成功，开始创建PaymentRecord");
                                    System.out.println(paymentRecord);
                                    return client.create(paymentRecord);
                                }else{
                                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建订单失败"));
                                }

                            });
                    })
                    .doOnSuccess(created -> System.out.println("订单创建成功"))
                    .doOnError(e -> System.err.println("创建订单失败: " + e.getMessage()))
                    .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建订单失败")));
            }))
            .flatMap(paymentRecord -> {
                System.out.println("查询到了，说明已经生成订单了，说明这个订单已经存在了，直接返回");
                PaymentRecordDto dto = new PaymentRecordDto();
                dto.setContentId(paymentRecord.getSpec().getContentId());
                dto.setClientId(paymentRecord.getSpec().getClientId());
                dto.setOrderId(paymentRecord.getSpec().getOrderId());
                dto.setPrice(paymentRecord.getSpec().getPrice());
                dto.setCreateTime(paymentRecord.getSpec().getCreateTime());
                dto.setPayStatus(paymentRecord.getSpec().getPayStatus());
                dto.setPreviewContent(paymentRecord.getSpec().getPreviewContent());
                dto.setExpireTime(paymentRecord.getSpec().getExpireTime());
                dto.setQrCodeUrl(MyUtils.generateQRCodeImage(paymentRecord.getSpec().getQrCodeUrl(), 200, 200));
                return Mono.just(dto);
            });

    }

    /**
     * 查询支付状态接口
     * 用于前端轮询检查支付是否完成
     *
     * @param contentId 订单ID
     * @return 包含支付状态的Map，包括：
     *         - status: 支付状态（PENDING/SUCCESS/FAILED）
     *         - message: 状态说明
     */
    @GetMapping("/purchase/status/{contentId}")
    public Mono<Map<String, String>> checkPurchaseStatus(@PathVariable("contentId") String contentId) {

        return client.fetch(PaymentRecord.class, contentId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在")))
            .flatMap(content -> {
                Map<String, String> response = new HashMap<>();
                String payStatus = content.getSpec().getPayStatus();

                if (PayStatus.PENDING.name().equals(payStatus)) {
                    //未支付
                    response.put("payStatus", PayStatus.PENDING.name());

                }else if (PayStatus.SUCCESS.name().equals(payStatus)) {
                    //已支付
                    response.put("payStatus", PayStatus.SUCCESS.name());
                }else if (PayStatus.FAILED.name().equals(payStatus)) {
                    //支付失败
                    response.put("payStatus", PayStatus.FAILED.name());
                }


                return Mono.just(response);
            });

        // // TODO: 实现实际的支付状态查询逻辑
        // return Mono.just(Map.of(
        //     "status", "PENDING",  // 可能的状态：PENDING, SUCCESS, FAILED
        //     "message", "等待支付"
        // ));
    }

    /**
     * 获取已购买内容列表接口
     * 返回当前用户已成功支付的所有内容ID列表
     *
     * @return 已购买内容ID的列表
     */
    @GetMapping("/purchased-contents")
    public Mono<List<String>> getPurchasedContents() {
        // 构建查询条件：查找支付状态为 SUCCESS 的记录
        Predicate<PaymentRecord> predicate = record -> 
            PayStatus.SUCCESS.name().equals(record.getSpec().getPayStatus());
        
        // 查询并返回所有已购买的内容ID
        return client.list(PaymentRecord.class, predicate, null)
            .map(record -> record.getMetadata().getName())
            .collectList();
    }
}




// // 构建查询条件：检查是否存在成功支付的记录
// Predicate<PaymentRecord> predicate = record -> {
//     Map<String, String> labels = record.getMetadata().getLabels();
//     return labels != null &&
//            contentId.equals(labels.get("content-id")) &&
//            "SUCCESS".equals(labels.get("status"));
// };
//
//
// ListOptions options = ListOptions.builder().fieldQuery(QueryFactory.equal("content-id", contentId)).build();
//
// // 查询支付记录
// return client.list(PaymentRecord.class, predicate, null)
//     .collectList()
//     .map(records -> {
//         Map<String, String> response = new HashMap<>();
//         String fullContent = content.getSpec().getContent();
//
//         if (!records.isEmpty()) {
//             // 已购买，返回完整内容
//             response.put("content", fullContent);
//             response.put("isPaid", "true");
//         } else {
//             // 未购买，返回预览内容和购买信息
//             response.put("content", generatePreview(fullContent));
//             response.put("isPaid", "false");
//             response.put("price", String.valueOf(content.getSpec().getPrice()));
//             response.put("message", "此内容需要付费才能查看完整内容");
//         }
//         return response;
//     });