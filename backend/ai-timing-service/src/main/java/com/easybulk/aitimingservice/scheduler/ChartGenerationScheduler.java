package com.easybulk.aitimingservice.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChartGenerationScheduler {

    // Tous les jours à 3h00
    @Scheduled(cron = "0 0 3 * * *")
    public void generateDailyCharts() {
        try {
            CommandLine cmd = new CommandLine("python3");
            cmd.addArgument("/app/python/generate_charts.py");
            cmd.addArgument("/app/models/resultats_phase2_v13.json");
            cmd.addArgument("/app/models/engagement_by_tier.json");

            DefaultExecutor executor = new DefaultExecutor();
            executor.setWatchdog(new ExecuteWatchdog(30_000));
            executor.execute(cmd);

            log.info("✅ Charts régénérés à {}", java.time.LocalDateTime.now());
        } catch (Exception e) {
            log.error("❌ Échec génération des charts", e);
        }
    }
}