package run.halo.starter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

// @Data
// @EqualsAndHashCode(callSuper = true)
// @GVK(group = "plugin.halo.run", version = "v1alpha1", kind = "PaywallContent", plural = "paywallcontents", singular = "paywallcontent")
// public class PaywallContent extends AbstractExtension {

    // @Schema(required = true)
    // private PaywallContentSpec spec;
    //
    // @Schema
    // private PaywallContentStatus status;
    //
    // @Data
    // public static class PaywallContentSpec {
    //     @Schema(required = true, description = "内容ID")
    //     private String contentId;
    //
    //     @Schema(required = true, description = "付费内容")
    //     private String content;
    //
    //     @Schema(required = true, description = "价格")
    //     private Double price;
    //
    //     @Schema(required = true, description = "创建时间")
    //     private Long createTime;
    //
    //     @Schema(required = true, description = "过期时间")
    //     private Long expireTime;
    // }
    //
    // @Data
    // public static class PaywallContentStatus {
    //     private String phase;
    //     private String message;
    // }
// }