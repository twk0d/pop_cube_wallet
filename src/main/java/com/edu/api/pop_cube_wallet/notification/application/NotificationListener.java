package com.edu.api.pop_cube_wallet.notification.application;

import com.edu.api.pop_cube_wallet.transaction.TransactionCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Notification module — the "Messenger" bounded context.
 * Purely reactive: triggered by TransactionCompletedEvent via Spring Modulith events.
 * MVP implementation: mock service that logs the notification (ADR-0008).
 * No direct coupling to the Transaction module.
 */
@Component
class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @ApplicationModuleListener
    void onTransactionCompleted(TransactionCompletedEvent event) {
        // Mock notification — in production this would send SMS, email, push, etc.
        log.info("""
                [NOTIFICATION] Transfer alert:
                  Transaction ID : {}
                  From           : {}
                  To             : {}
                  Amount         : {}
                  Completed at   : {}""",
                event.transactionId(),
                event.sourceAccountId(),
                event.destinationAccountId(),
                event.amount(),
                event.completedAt()
        );
    }
}
