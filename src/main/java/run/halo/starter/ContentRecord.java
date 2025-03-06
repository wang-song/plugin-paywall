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
    kind = "ContentRecord",
    plural = "contentrecords",
    singular = "contentrecord")
public class ContentRecord extends AbstractExtension {

    //name为利用contentId生成的UUID，MyUtils.generateDeterministicUUID

    private ContentRecordSpec spec;

    @Data
    @Schema(name = "ContentRecordSpec")
    public static class ContentRecordSpec {
        
        @Schema(description = "内容ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String contentId;

        @Schema(description = "支付金额", requiredMode = Schema.RequiredMode.REQUIRED)
        private Double price;

        @Schema(description = "付费内容",requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;

        @Schema(description = "预览内容")
        private String previewContent;

    }
} 