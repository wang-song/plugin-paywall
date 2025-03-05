package run.halo.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.theme.ReactivePostContentHandler;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaywallPostContentHandler implements ReactivePostContentHandler {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<PostContentContext> handle(PostContentContext postContentContext) {
        return Mono.just(postContentContext)
            .flatMap(context -> {
                try {
                    String content = context.getContent();
                    Document doc = Jsoup.parse(content);
                    
                    // 处理付费内容区块
                    Elements paywallElements = doc.select("div[data-type=paywall]");
                    log.info("找到 {} 个付费模块", paywallElements.size());
                    
                    if (!paywallElements.isEmpty()) {
                        // 只有在有付费内容时才注入样式和脚本
                        Element head = doc.head();
                        if (head == null) {
                            head = doc.createElement("head");
                            doc.prependChild(head);
                        }

                        // 添加 CSS
                        Element cssLink = doc.createElement("link");
                        cssLink.attr("rel", "stylesheet");
                        cssLink.attr("href", "/plugins/paywall/assets/res/paywall.css");
                        head.appendChild(cssLink);
                        
                        // 添加 JS
                        Element script = doc.createElement("script");
                        script.attr("src", "/plugins/paywall/assets/res/paywall.js");
                        head.appendChild(script);
                    }
                    
                    // 创建一个 Mono 序列来处理所有付费内容
                    return Mono.fromSupplier(() -> {
                        for (Element element : paywallElements) {
                            try {
                                String price = element.attr("data-price");
                                String previewContent = element.attr("data-preview");
                                String originalContent = element.html();
                                
                                log.info("处理付费内容：price={}, preview={}", price, previewContent);
                                
                                // 生成内容ID
                                String contentId = UUID.randomUUID().toString();
                                
                                // 创建付费内容容器
                                Element container = new Element("div")
                                    .attr("class", "paywall-container");
                                
                                // 创建预览区域
                                Element previewArea = new Element("div")
                                    .attr("class", "paywall-preview");
                                if (previewContent != null && !previewContent.isEmpty()) {
                                    previewArea.html(previewContent);
                                }
                                // else {
                                //     // 如果没有指定预览内容，截取原始内容的前100个字符
                                //     String preview = originalContent.length() > 100 ?
                                //         originalContent.substring(0, 100) + "..." :
                                //         originalContent;
                                //     previewArea.html(preview);
                                // }
                                container.appendChild(previewArea);
                                
                                // 创建付费提示区域
                                Element paymentArea = new Element("div")
                                    .attr("class", "paywall-payment-area");
                                
                                Element paymentInfo = new Element("div")
                                    .attr("class", "paywall-payment-info")
                                    .html("此处为付费内容，价格：" + price + " 元");
                                
                                Element purchaseButton = new Element("button")
                                    .attr("class", "paywall-purchase-btn")
                                    .attr("data-content-id", contentId)
                                    .attr("data-price", price)
                                    .attr("onclick", "handlePurchase('" + contentId + "')")
                                    .html("购买完整内容");
                                
                                paymentArea.appendChild(paymentInfo);
                                paymentArea.appendChild(purchaseButton);
                                container.appendChild(paymentArea);
                                
                                // 创建空的内容区域（初始隐藏）
                                Element contentArea = new Element("div")
                                    .attr("class", "paywall-content")
                                    .attr("style", "display: none;")
                                    .attr("id", "content-" + contentId)
                                    .html(""); // 确保内容区域为空
                                container.appendChild(contentArea);
                                
                                // 替换原始元素
                                element.replaceWith(container);
                                
                                // 创建 PaymentRecord 资源
                                PaymentRecord paymentRecord = new PaymentRecord();
                                paymentRecord.setMetadata(new run.halo.app.extension.Metadata());
                                // 为资源设置名称 用于查询
                                paymentRecord.getMetadata().setName(contentId);

                                PaymentRecord.PaymentRecordSpec spec = new PaymentRecord.PaymentRecordSpec();
                                // 为资源设置内容ID
                                spec.setContentId(contentId);
                                // 为资源付费内容
                                spec.setContent(originalContent);
                                // 为资源设置预览内容
                                spec.setPreviewContent(previewContent);
                                // 为资源付费内容设置价格
                                spec.setPrice(Double.parseDouble(price));
                                // 为资源设置创建时间
                                spec.setCreateTime(Instant.now().getEpochSecond());
                                // spec.setExpireTime(Instant.now().plusSeconds(24 * 60 * 60).getEpochSecond());


                                spec.setPayStatus(PayStatus.PENDING.name());

                                paymentRecord.setSpec(spec);

                                // 异步保存到 Kubernetes
                                client.create(paymentRecord)
                                    .doOnSuccess(created -> log.info("成功创建付费内容资源：contentId={}", contentId))
                                    .doOnError(error -> log.error("创建付费内容资源失败：", error))
                                    .subscribe();
                                
                            } catch (Exception e) {
                                log.error("处理付费内容区块失败：", e);
                            }
                        }
                        
                        context.setContent(doc.html());
                        return context;
                    });
                } catch (Exception e) {
                    log.error("处理文章内容失败：", e);
                    return Mono.error(e);
                }
            });
    }
} 