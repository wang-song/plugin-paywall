package run.halo.starter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(group = "plugin-paywall.halo.run", 
    version = "v1alpha1", 
    kind = "MqSetting",
    plural = "mqsettings",
    singular = "mqsetting")
public class MqSetting extends AbstractExtension {


    private MqSettingSpec spec;

    @Data
    @Schema(name = "MqSettingSpec")
    public static class MqSettingSpec {
        
        @Schema(description = "服务器地址", requiredMode = Schema.RequiredMode.REQUIRED)
        private String serverUrl;


        @Schema(description = "通讯密钥",requiredMode = Schema.RequiredMode.REQUIRED)
        private String key;

        @Schema(description = "回调地址")
        private String notifyUrl;

        @Schema(description = "是否是https")
        private boolean isHttps;

    }
} 