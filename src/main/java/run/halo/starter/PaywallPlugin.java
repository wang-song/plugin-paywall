package run.halo.starter;

import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;

@Component
public class PaywallPlugin extends BasePlugin {
    private final SchemeManager schemeManager;

    public PaywallPlugin(PluginWrapper wrapper, SchemeManager schemeManager) {
        super(wrapper);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(PaymentRecord.class);
        schemeManager.register(PaywallContent.class);
    }

    @Override
    public void stop() {
        // 插件停止时的清理工作
    }
} 