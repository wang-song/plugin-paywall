package run.halo.starter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;


@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "plugin-encrypt.halo.run", version = "v1alpha1", kind = "EncryptedContent", plural = "encryptedcontents", singular = "encryptedcontent")
public class EncryptedContent extends AbstractExtension {

    @Schema(description = "加密内容的规格")
    private EncryptedContentSpec spec;


    @Data
    @Schema(name = "EncryptedContentSpec")
    public static class EncryptedContentSpec {

        @Schema(description = "UUID")
        private String uuid;

        @Schema(description = "加密的内容")
        private String encryptedText;

        @Schema(description = "加密密码")
        private String password;


        @Schema(description = "是否锁定")
        private Boolean locked;

        @Schema(description = "锁定时间（时间戳）")
        private Long lockTime;

        @Schema(description = "当前验证尝试次数")
        private Integer currentAttempts;

        @Schema(description = "最后一次尝试时间")
        private Long lastAttemptTime;

    }


}
