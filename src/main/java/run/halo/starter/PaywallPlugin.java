package run.halo.starter;

import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexAttributeFactory;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

@Component
public class PaywallPlugin extends BasePlugin {
    @Autowired
    private SchemeManager schemeManager;

    public PaywallPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        schemeManager.register(ContentRecord.class);
        schemeManager.register(MqSetting.class);
        schemeManager.register(PaymentRecord.class,indexSpecs -> {
            indexSpecs.add(new IndexSpec().setName("spec.orderId")
                .setIndexFunc(
                    simpleAttribute(PaymentRecord.class, record -> record.getSpec().getOrderId())
                ));
            indexSpecs.add(new IndexSpec().setName("spec.expireTime")
                .setIndexFunc(
                    simpleAttribute(PaymentRecord.class, record -> String.valueOf(
                        record.getSpec().getExpireTime()))
                ));
            indexSpecs.add(new IndexSpec().setName("spec.payStatus")
                .setIndexFunc(
                    simpleAttribute(PaymentRecord.class, record -> record.getSpec().getPayStatus())
                ));

            indexSpecs.add(new IndexSpec().setName("spec.contentId")
                .setIndexFunc(
                    simpleAttribute(PaymentRecord.class, record -> record.getSpec().getContentId())
                ));

            indexSpecs.add(new IndexSpec().setName("spec.clientId")
                .setIndexFunc(
                    simpleAttribute(PaymentRecord.class, record -> record.getSpec().getClientId())
                ));
        });


    }

    @Override
    public void stop() {
        // 插件停止时的清理工作
        schemeManager.unregister(schemeManager.get(ContentRecord.class));
        schemeManager.unregister(schemeManager.get(MqSetting.class));
        schemeManager.unregister(schemeManager.get(PaymentRecord.class));
    }
} 