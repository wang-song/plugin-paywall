package run.halo.starter.test;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ApiVersion;

import java.util.Map;
import java.util.Optional;

// import static run.halo.starter.CachedEncrptyPostContentHandler.findInCache;

@RestController
@RequiredArgsConstructor
@ApiVersion("plugin-encrypt.halo.run/v1alpha1")
@RequestMapping("/encrypt")
public class DecryptController {


    // @PostMapping("/decrypt")
    // public Mono<ResponseEntity<Map< String,  String>>> decrypt(@RequestBody DecryptRequest request) {
    //
    //     CachedEncryptedContent content = findInCache(request.getContentHash());
    //
    //     if(Optional.ofNullable(content).isEmpty()) {
    //         return Mono.just(ResponseEntity.status(404)
    //             .body(Map.of("message", "内容不存在")));
    //
    //     }else {
    //         if (content.getPassword().equals(request.getPassword())) {
    //             return Mono.just(ResponseEntity.ok()
    //                 .body(Map.of(
    //                     "content", content.getEncryptedText(),
    //                     "message", "解密成功"
    //                 )));
    //         } else {
    //             return Mono.just(ResponseEntity.badRequest()
    //                 .body(Map.of("message", "密码错误，请重试")));
    //         }
    //     }
    //
    // }

    // @PostMapping("/decrypt")
    // public Mono<ResponseEntity<Map< String,  String>>> decrypt(@RequestBody DecryptRequest request) {
    //
    //
    //     return  client.get(EncryptedContent.class, request.getEncryptName())
    //         .flatMap(content -> {
    //
    //             if (content.getSpec().getPassword().equals(request.getPassword())) {
    //                 return Mono.just(ResponseEntity.ok()
    //                     .body(Map.of(
    //                         "content", content.getSpec().getEncryptedText(),
    //                         "message", "解密成功"
    //                     )));
    //             } else {
    //                 return Mono.just(ResponseEntity.badRequest()
    //                     .body(Map.of("message", "密码错误，请重试")));
    //             }
    //         })
    //         .switchIfEmpty(Mono.just(ResponseEntity.status(404)
    //             .body(Map.of("message", "内容不存在"))));
    //
    // }
}


