package run.halo.starter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.app.theme.ReactivePostContentHandler;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import run.halo.app.extension.ListOptions;
import java.util.Map;

import reactor.core.publisher.Flux;

/**
 * 加密内容处理器
 * 用于处理文章中的加密内容区域,将其转换为带密码保护的可交互区域
 */
@RequiredArgsConstructor
@Component
public class EncrptyPostContentHandler implements ReactivePostContentHandler {

    /**
     * Halo提供的响应式扩展客户端,用于操作自定义模型
     */
    private final ReactiveExtensionClient reactiveExtensionClient;

    /**
     * 处理文章内容,识别并处理加密区域
     * @param postContentContext 文章内容上下文
     * @return 处理后的文章内容
     */
    @Override
    public Mono<PostContentContext> handle(PostContentContext postContentContext) {
        // 获取原始文章内容
        String content = postContentContext.getContent();

        // 使用Jsoup解析HTML内容
        Document doc = Jsoup.parse(content);
        // 查找所有带有data-type=encrypt属性的div元素,这些是需要加密的内容区域
        Elements encryptDivs = doc.select("div[data-type=encrypt]");

        // 在文档头部添加加密处理的JavaScript脚本
        Element head = doc.head();
        if (head == null) {
            head = doc.createElement("head");
            doc.prependChild(head);
        }
        Element script = doc.createElement("script");
        script.attr("src", "/plugins/plugin-encrypt/assets/res/encrypt-plugin.js");
        head.appendChild(script);

        // 使用Flux处理每个加密区域
        return Flux.fromIterable(encryptDivs)
            .flatMap(div -> {
                // 获取加密区域的密码和内容
                String password = div.attr("data-password");
                String encryptedContentHtml = div.html();
                // 使用内容的哈希值作为唯一标识
                String contentHash = String.valueOf(div.html().hashCode());

                // 构建查询条件,使用标签选择器查找是否已存在相同内容的加密对象
                ListOptions options = ListOptions.builder()
                    .labelSelector()
                    .eq("content-hash", contentHash)
                    .end()
                    .build();

                // 查询数据库中是否存在相同内容的加密对象
                return reactiveExtensionClient.listAll(EncryptedContent.class, options, null)
                    .collectList()
                    .flatMap(list -> {
                        if (!list.isEmpty()) {
                            // 如果存在,直接使用已有的加密对象
                            return Mono.just(list.get(0));
                        } else {
                            // 如果不存在,创建新的加密对象
                            EncryptedContent newContent = new EncryptedContent();
                            newContent.setMetadata(new Metadata());
                            // 使用内容哈希作为名称的一部分
                            String name = "encrypted-" + contentHash;
                            newContent.getMetadata().setName(name);

                            // 设置内容哈希标签,用于后续查询
                            newContent.getMetadata().setLabels(Map.of(
                                "content-hash", contentHash
                            ));

                            // 设置加密对象的具体内容
                            EncryptedContent.EncryptedContentSpec spec = new EncryptedContent.EncryptedContentSpec();
                            spec.setPassword(password);          // 设置密码
                            spec.setEncryptedText(encryptedContentHtml);  // 设置加密内容
                            spec.setUuid(UUID.randomUUID().toString());   // 生成唯一标识符

                            newContent.setSpec(spec);

                            // 将新创建的加密对象保存到数据库
                            return reactiveExtensionClient.create(newContent);
                        }
                    })
                    .map(encryptedContent -> {
                        // 创建新的加密内容显示元素
                        Element newDiv = doc.createElement("div");
                        newDiv.addClass("encrypted-content");
                        // 将加密对象的名称保存在data-id属性中
                        newDiv.attr("data-id", encryptedContent.getMetadata().getName());

                        // 构建加密内容的HTML结构
                        // 包含:
                        // 1. 加密提示头部
                        // 2. 密码输入框和解锁按钮
                        // 3. 错误信息显示区域
                        // 4. 解密后内容显示区域
                        newDiv.html(String.format("""
                            <div class="encrypt-header">
                                <i class="encrypt-icon">🔒</i>
                                <span>加密内容，请输入密码查看</span>
                            </div>
                            <div class="encrypt-input">
                                <input type="password" placeholder="请输入访问密码" id="pwd-%s">
                                <button onclick="decryptContent('%s','%s')">解锁查看</button>
                            </div>
                            <div class="encrypt-error" style="display:none"></div>
                            <div id="content-%s" style="display:none"></div>
                            """,
                            encryptedContent.getSpec().getUuid(),
                            encryptedContent.getSpec().getUuid(),
                            encryptedContent.getMetadata().getName(),
                            encryptedContent.getSpec().getUuid()));

                        // 用新创建的加密显示元素替换原始的加密div
                        div.replaceWith(newDiv);
                        return newDiv;
                    });
            })
            // 处理完所有加密区域后,返回更新后的文章内容
            .then(Mono.just(postContentContext))
            .doOnNext(ctx -> ctx.setContent(doc.html()));
    }
}
