package run.halo.starter;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONObjectIter;
import cn.hutool.json.JSONUtil;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ApiVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 付费内容 API 端点
 * 提供付费内容的访问、购买和支付状态查询功能
 */
@ApiVersion("plugin-paywall.halo.run/v1alpha1")
@RestController
@RequestMapping("/setting")
@RequiredArgsConstructor
public class SettingEndpoint {


    private final ReactiveExtensionClient client;
    // private final ExtensionClient clientTB;

    @GetMapping("/getSettings")
    public Mono<MqSetting> getSetting() {
        System.out.println("getSettings");
        // 首先获取内容信息
       return client.fetch(MqSetting.class, "mqsetting")
            .hasElement()
            .flatMap(hasElement -> {
                if (hasElement) {
                    System.out.println("获取setting");
                    return client.fetch(MqSetting.class, "mqsetting");
                } else {
                    System.out.println("setting不存在");
                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在"));
                }

            });

    }


    @RequestMapping("/saveSettings/{settingString}")
    public Mono<MqSetting> saveSetting(@PathVariable("settingString") String settingString) {

        JSONObject jsonObject = JSONUtil.parseObj(settingString);
        String serverUrl = jsonObject.getStr("serverUrl", "");
        String key = jsonObject.getStr("key", "");
        String notifyUrl = jsonObject.getStr("notifyUrl", "");
        // 首先获取内容信息
        return client.fetch(MqSetting.class, "mqsetting")
            .hasElement()
            .flatMap(hasElement -> {
                if (hasElement) {
                    return client.fetch(MqSetting.class, "mqsetting")
                        .flatMap(setting1 -> {
                            System.out.println("更新setting");
                            MqSetting.MqSettingSpec mqSettingSpec = setting1.getSpec();
                            mqSettingSpec.setServerUrl(serverUrl);
                            mqSettingSpec.setKey(key);
                            mqSettingSpec.setNotifyUrl(notifyUrl);
                            setting1.setSpec(mqSettingSpec);
                            return client.update(setting1).doOnError(error -> {
                                System.out.println("更新setting失败");
                            });
                        });
                } else {
                    System.out.println("创建setting");
                    MqSetting setting = new MqSetting();
                    setting.setMetadata(new run.halo.app.extension.Metadata());
                    setting.getMetadata().setName("mqsetting");

                    MqSetting.MqSettingSpec mqSettingSpec = new MqSetting.MqSettingSpec();
                    mqSettingSpec.setServerUrl(serverUrl);
                    mqSettingSpec.setKey(key);
                    mqSettingSpec.setNotifyUrl(notifyUrl);
                    setting.setSpec(mqSettingSpec);
                    return client.create(setting).doOnError(error1 -> {
                        System.out.println("创建setting失败");
                    });
                }
            }).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在")));
    }
}



