package run.halo.starter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
public class PaymentRecordDto  {


        private String contentId;


        private String clientId;

        private String orderId;

        private Double price;

        @Schema(description = "支付状态: PENDING/SUCCESS/FAILED")
        private String payStatus;


        private Long createTime;


        private Long payTime;


        private String qrCodeUrl;


        private String previewContent;

        private String content;

    private Long expireTime;


} 