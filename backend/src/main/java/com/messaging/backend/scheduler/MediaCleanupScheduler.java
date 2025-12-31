/*
 * Media Cleanup Scheduler
 * 
 * Scheduled job for TTL-based media deletion.
 * Runs every 6 hours to delete expired media files.
 * 
 * Similar to WhatsApp's 30-day media expiry.
 */
package com.messaging.backend.scheduler;

import com.messaging.backend.service.MediaStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MediaCleanupScheduler {

    @Autowired
    private MediaStorageService mediaStorageService;

    /**
     * Runs every 6 hours to clean up expired media
      * Cron: every 6 hours at minute 0 (expression: 0 0 0/6 * * ?)
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void cleanupExpiredMedia() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println("[" + timestamp + "] Starting media cleanup job...");
        
        try {
            int deletedCount = mediaStorageService.deleteExpiredMedia();
            System.out.println("[" + timestamp + "] Media cleanup complete: " + deletedCount + " files deleted");
        } catch (Exception e) {
            System.err.println("[" + timestamp + "] Media cleanup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Optional: Run cleanup on startup (after 1 minute delay)
     * Useful for catching any missed cleanups during downtime
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    public void startupCleanup() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println("[" + timestamp + "] Running startup media cleanup...");
        
        try {
            int deletedCount = mediaStorageService.deleteExpiredMedia();
            if (deletedCount > 0) {
                System.out.println("[" + timestamp + "] Startup cleanup: " + deletedCount + " expired files deleted");
            } else {
                System.out.println("[" + timestamp + "] Startup cleanup: No expired files found");
            }
        } catch (Exception e) {
            System.err.println("[" + timestamp + "] Startup cleanup failed: " + e.getMessage());
        }
    }
}
