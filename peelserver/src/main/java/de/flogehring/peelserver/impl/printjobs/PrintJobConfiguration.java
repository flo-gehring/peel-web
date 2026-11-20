package de.flogehring.peelserver.impl.printjobs;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableAsync
class PrintJobConfiguration {
    @Bean
    ThreadPoolTaskExecutor printJobExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("print-job-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean
    InitializingBean recoverInterruptedPrintJobs(MongoTemplate mongo) {
        // Only one backend can run against this database. Any running job at startup
        // belonged to the previous process, which held its resolved inputs in memory.
        return () -> mongo.updateMulti(
                Query.query(Criteria.where("printJobPersistenceData.status")
                        .in(PrintJobStatus.CALCULATING, PrintJobStatus.PRINTING)),
                new Update().set("printJobPersistenceData.status", PrintJobStatus.FAILED)
                        .set("printJobPersistenceData.fileIds", List.of()),
                PrintJobPersistence.class);
    }
}
