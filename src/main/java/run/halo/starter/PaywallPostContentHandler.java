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

    private static final String PAYWALL_STYLES = """
            <style>
            .paywall-container {
                margin: 20px 0;
                padding: 20px;
                border: 1px solid #e0e0e0;
                border-radius: 8px;
                background-color: #f9f9f9;
            }
            .paywall-preview {
                margin-bottom: 15px;
                color: #666;
            }
            .paywall-payment-area {
                text-align: center;
                padding: 15px;
                background-color: #fff;
                border-radius: 4px;
            }
            .paywall-payment-info {
                margin-bottom: 10px;
                color: #333;
                font-weight: bold;
            }
            .paywall-purchase-btn {
                padding: 8px 20px;
                background-color: #007bff;
                color: white;
                border: none;
                border-radius: 4px;
                cursor: pointer;
                font-size: 14px;
            }
            .paywall-purchase-btn:hover {
                background-color: #0056b3;
            }
            .paywall-content {
                display: none;
            }
            </style>
            """;

    private static final String PAYWALL_SCRIPT = """
            <script>
            function handlePurchase(contentId) {
                // 获取价格
                const button = document.querySelector(`button[data-content-id="${contentId}"]`);
                const price = button.getAttribute('data-price');
                
                // 调用购买接口
                fetch(`/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-paywall/purchase/${contentId}`, {
                    method: 'POST'
                })
                .then(response => response.json())
                .then(data => {
                    // 显示支付二维码
                    alert('请扫码支付：' + data.qrCodeUrl + '\\n价格：' + price + ' 元');
                    
                    // 开始轮询支付状态
                    checkPaymentStatus(contentId, data.orderId);
                })
                .catch(error => {
                    console.error('购买请求失败:', error);
                    alert('购买请求失败，请稍后重试');
                });
            }

            function checkPaymentStatus(contentId, orderId) {
                const checkStatus = () => {
                    fetch(`/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-paywall/purchase/status/${orderId}`)
                        .then(response => response.json())
                        .then(data => {
                            if (data.status === 'SUCCESS') {
                                // 支付成功，获取内容
                                fetchContent(contentId);
                            } else if (data.status === 'PENDING') {
                                // 继续轮询
                                setTimeout(checkStatus, 3000);
                            } else {
                                alert('支付失败：' + data.message);
                            }
                        })
                        .catch(error => {
                            console.error('检查支付状态失败:', error);
                            alert('检查支付状态失败，请刷新页面重试');
                        });
                };
                
                checkStatus();
            }

            function fetchContent(contentId) {
                fetch(`/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-paywall/content/${contentId}`)
                    .then(response => response.json())
                    .then(data => {
                        if (data.isPaid === 'true') {
                            // 更新内容
                            const container = document.querySelector(`#content-${contentId}`);
                            container.innerHTML = data.content;
                            container.style.display = 'block';
                            
                            // 隐藏支付区域
                            const paymentArea = container.parentElement.querySelector('.paywall-payment-area');
                            paymentArea.style.display = 'none';
                            
                            // 隐藏预览区域
                            const previewArea = container.parentElement.querySelector('.paywall-preview');
                            previewArea.style.display = 'none';
                        }
                    })
                    .catch(error => {
                        console.error('获取内容失败:', error);
                        alert('获取内容失败，请刷新页面重试');
                    });
            }
            </script>
            """;

    @Override
    public Mono<PostContentContext> handle(PostContentContext postContentContext) {
        return Mono.just(postContentContext)
            .flatMap(context -> {
                try {
                    String content = context.getContent();
                    Document doc = Jsoup.parse(content);
                    
                    // 处理付费内容区块
                    Elements paywallElements = doc.select("div[data-type=paywall]");
                    log.info("找到 {} 个付费内容区块", paywallElements.size());
                    
                    if (!paywallElements.isEmpty()) {
                        // 只有在有付费内容时才注入样式和脚本
                        Element body = doc.body();
                        body.append(PAYWALL_STYLES);
                        body.append(PAYWALL_SCRIPT);
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
                                } else {
                                    // 如果没有指定预览内容，截取原始内容的前100个字符
                                    String preview = originalContent.length() > 100 ? 
                                        originalContent.substring(0, 100) + "..." : 
                                        originalContent;
                                    previewArea.html(preview);
                                }
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
                                
                                // 创建 PaywallContent 资源
                                PaywallContent paywallContent = new PaywallContent();
                                paywallContent.setMetadata(new run.halo.app.extension.Metadata());
                                paywallContent.getMetadata().setName(contentId);
                                
                                PaywallContent.PaywallContentSpec spec = new PaywallContent.PaywallContentSpec();
                                spec.setContentId(contentId);
                                spec.setContent(originalContent);
                                spec.setPrice(Double.parseDouble(price));
                                spec.setCreateTime(Instant.now().getEpochSecond());
                                spec.setExpireTime(Instant.now().plusSeconds(24 * 60 * 60).getEpochSecond());
                                
                                paywallContent.setSpec(spec);
                                
                                // 异步保存到 Kubernetes
                                client.create(paywallContent)
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