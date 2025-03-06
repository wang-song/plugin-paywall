package run.halo.starter;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ApiVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 付费内容 API 端点
 * 提供付费内容的访问、购买和支付状态查询功能
 */
@ApiVersion("plugin-paywall.halo.run/v1alpha1")
@RestController
@RequestMapping("/setting")
@RequiredArgsConstructor
public class SettingEndpoint {

    /**
     * Kubernetes 资源操作客户端
     * 用于操作 PaywallContent 和 PaymentRecord 资源
     */
    private final ReactiveExtensionClient client;

    @GetMapping("/getSettings")
    public Mono<MqSetting> getContent() {
        // 首先获取内容信息
        return client.fetch(MqSetting.class, "plugin-paywall.halo.run.v1alpha1.mqsetting")
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在")))
            .flatMap(Mono::just);
    }


}



