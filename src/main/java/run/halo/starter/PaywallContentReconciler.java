package run.halo.starter;

import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.controller.Reconciler.Request;

import java.time.Duration;
import java.time.Instant;

@Component
public class PaywallContentReconciler implements Reconciler<Request> {
    
    private final ExtensionClient client;
    
    public PaywallContentReconciler(ExtensionClient client) {
        this.client = client;
    }

    @Override
    public Result reconcile(Request request) {
        return client.fetch(PaywallContent.class, request.name())
            .map(content -> {
                // 检查是否过期
                if (content.getSpec().getExpireTime() <= Instant.now().getEpochSecond()) {
                    // 删除过期内容
                    client.delete(content);
                    return new Result(false, Duration.ofSeconds(0));
                }
                
                // 更新状态
                content.setStatus(new PaywallContent.PaywallContentStatus());
                content.getStatus().setPhase("Active");
                content.getStatus().setMessage("Content is active");
                client.update(content);
                
                // 计算下次检查时间
                long nextCheck = content.getSpec().getExpireTime() - Instant.now().getEpochSecond();
                return new Result(false, Duration.ofSeconds(Math.max(1, nextCheck)));
            })
            .orElseGet(() -> new Result(false, Duration.ofSeconds(0)));
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new PaywallContent())
            .build();
    }
} 