package run.halo.starter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.util.Map;
import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "plugin-paywall.halo.run", 
    version = "v1alpha1", 
    kind = "PaymentRecord", 
    plural = "paymentrecords", 
    singular = "paymentrecord")
public class PaymentRecord extends AbstractExtension {

    @Schema(description = "付费内容记录的规格")
    private PaymentRecordSpec spec;

    @Data
    @Schema(name = "PaymentRecordSpec")
    public static class PaymentRecordSpec {
        
        @Schema(description = "内容ID(由content-hash生成)", requiredMode = Schema.RequiredMode.REQUIRED)
        private String contentId;

        @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
        private String orderId;

        @Schema(description = "支付金额", requiredMode = Schema.RequiredMode.REQUIRED)
        private String price;

        @Schema(description = "支付状态: PENDING/SUCCESS/FAILED", requiredMode = Schema.RequiredMode.REQUIRED)
        private String status;

        @Schema(description = "创建时间")
        private Long createTime;

        @Schema(description = "支付时间")
        private Long payTime;

        @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;

        @Schema(description = "QR码URL")
        private String qrCodeUrl;
    }

    public void updateLabels() {
        if (this.spec != null) {
            Map<String, String> labels = getMetadata().getLabels();
            if (labels == null) {
                labels = new HashMap<>();
                getMetadata().setLabels(labels);
            }
            labels.put("content-id", this.spec.getContentId());
            labels.put("username", this.spec.getUsername());
        }
    }
} 