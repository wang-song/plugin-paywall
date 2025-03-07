package run.halo.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.theme.ReactivePostContentHandler;

import java.time.Instant;
import java.util.Optional;
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

                                log.info("处理付费内容：price={}, preview={}", price,
                                    previewContent);

                                // 生成内容ID
                                String contentId = context.getPost().getMetadata().getName();

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
                                context.setContent(doc.html());

                                String uuid = MyUtils.generateDeterministicUUID(contentId);
                                //先查询有没有这个资源

                                // return client.fetch(ContentRecord.class, uuid)
                                //     .hasElement()
                                //     .flatMap(hasElement -> {
                                //         if (hasElement) {
                                //             log.info("资源已存在：contentId={}", uuid);
                                //             return Mono.empty();
                                //         } else {
                                //             // 创建 ContentRecord 资源
                                //             ContentRecord contentRecord = new ContentRecord();
                                //             contentRecord.setMetadata(
                                //                 new run.halo.app.extension.Metadata());
                                //             // 为资源设置名称 用于查询
                                //             contentRecord.getMetadata().setName(uuid);
                                //
                                //             ContentRecord.ContentRecordSpec spec =
                                //                 new ContentRecord.ContentRecordSpec();
                                //             spec.setContentId(uuid);
                                //             spec.setPrice(Double.parseDouble(price));
                                //             spec.setContent(originalContent);
                                //             spec.setPreviewContent(previewContent);
                                //             contentRecord.setSpec(spec);
                                //             return client.create(contentRecord);
                                //         }
                                //     });


                                client.fetch(ContentRecord.class, uuid)

                                    .switchIfEmpty(Mono.error(new RuntimeException
                                    ("ContentRecord资源不存在")))
                                    .doOnSuccess(contentRecord -> {
                                        log.info("资源已存在：contentId={}", uuid);
                                    })
                                    .doOnError(error -> {
                                        // 创建 ContentRecord 资源
                                        ContentRecord contentRecord = new ContentRecord();
                                        contentRecord.setMetadata(new run.halo.app.extension
                                        .Metadata());
                                        // 为资源设置名称 用于查询
                                        contentRecord.getMetadata().setName(uuid);

                                        ContentRecord.ContentRecordSpec spec = new
                                        ContentRecord.ContentRecordSpec();
                                        spec.setContentId(uuid);
                                        spec.setPrice(Double.parseDouble(price));
                                        spec.setContent(originalContent);
                                        spec.setPreviewContent(previewContent);
                                        contentRecord.setSpec(spec);
                                        client.create(contentRecord)
                                            .doOnSuccess(created -> log.info
                                            ("成功创建付费内容资源：contentId={}", uuid))
                                            .doOnError(error1 -> log.error("创建付费内容资源失败：", error))
                                            .subscribe();
                                    })
                                    .subscribe();


                            } catch (Exception e) {
                                log.error("处理付费内容区块失败：", e);
                            }
                        }


                        return context;
                    });
                } catch (Exception e) {
                    log.error("处理文章内容失败：", e);
                    return Mono.error(e);
                }
            });
    }
} 