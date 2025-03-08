package run.halo.starter;

import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.index.query.Query;
import java.util.List;

import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.lessThan;
import static run.halo.app.extension.index.query.QueryFactory.or;

@Component
@RequiredArgsConstructor
public class QuartzService {


    private final ExtensionClient client;
    @PostConstruct
    public void init() {
        CronUtil.schedule("*/3 * * * * *", new Task() {
            @Override
            public void execute() {
                try {
                    Query query = and(equal("spec.payStatus", PayStatus.PENDING.name()),lessThan("spec.expireTime", String.valueOf(System.currentTimeMillis())));

                    ListOptions options = ListOptions.builder().fieldQuery(query).build();

                    List<PaymentRecord> records = client.listAll(PaymentRecord.class, options, Sort.by(
                        Sort.Order.asc("spec.orderId")) );
                    Flux.fromIterable(records)
                        .flatMap(record -> Mono.fromRunnable(() -> client.delete(record)))
                        .collectList()
                        .subscribe();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

}
