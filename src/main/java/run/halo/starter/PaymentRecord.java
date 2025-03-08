package run.halo.starter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import java.util.Map;
import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(group = "plugin-paywall.halo.run", 
    version = "v1alpha1", 
    kind = "PaymentRecord", 
    plural = "paymentrecords", 
    singular = "paymentrecord")
public class PaymentRecord extends AbstractExtension {

    //name为clientId和contentId生成的UUID，MyUtils.generateDeterministicUUID

    @Schema(description = "付费内容记录的规格")
    private PaymentRecordSpec spec;

    @Data
    @Schema(name = "PaymentRecordSpec")
    public static class PaymentRecordSpec {
        
        @Schema(description = "内容ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String contentId;

        @Schema(description = "客户端ID")
        private String clientId;

        //创建索引，按照订单号查询是否支付成功
        @Schema(description = "订单号")
        private String orderId;

        @Schema(description = "支付金额", requiredMode = Schema.RequiredMode.REQUIRED)
        private Double price;

        @Schema(description = "支付状态: PENDING/SUCCESS/FAILED")
        private String payStatus;

        @Schema(description = "创建时间")
        private Long createTime;

        @Schema(description = "支付时间")
        private Long payTime;

        @Schema(description = "QR码URL")
        private String qrCodeUrl;

        @Schema(description = "付费内容",requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;

        @Schema(description = "预览内容")
        private String previewContent;

        //过期时间
        @Schema(description = "过期时间")
        private Long expireTime;





    }

    // public void updateLabels() {
    //     if (this.spec != null) {
    //         Map<String, String> labels = getMetadata().getLabels();
    //         if (labels == null) {
    //             labels = new HashMap<>();
    //             getMetadata().setLabels(labels);
    //         }
    //         labels.put("content-id", this.spec.getContentId());
    //         labels.put("username", this.spec.getUsername());
    //     }
    // }
} 