package run.halo.starter;

import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

@Component
public class PaywallPlugin extends BasePlugin {
    @Autowired
    private SchemeManager schemeManager;

    public PaywallPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        schemeManager.register(PaymentRecord.class);
        schemeManager.register(ContentRecord.class);
        schemeManager.register(MqSetting.class);
    }

    @Override
    public void stop() {
        // 插件停止时的清理工作
    }
} 