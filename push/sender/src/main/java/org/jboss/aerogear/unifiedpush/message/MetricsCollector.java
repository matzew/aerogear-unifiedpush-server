/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.unifiedpush.message;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.Queue;

import org.jboss.aerogear.unifiedpush.api.PushMessageInformation;
import org.jboss.aerogear.unifiedpush.api.VariantMetricInformation;
import org.jboss.aerogear.unifiedpush.message.holder.PushMessageCompleted;
import org.jboss.aerogear.unifiedpush.message.holder.VariantCompleted;
import org.jboss.aerogear.unifiedpush.message.jms.AbstractJMSMessageConsumer;
import org.jboss.aerogear.unifiedpush.message.jms.Dequeue;
import org.jboss.aerogear.unifiedpush.service.metrics.PushMessageMetricsService;

/**
 * Receives metrics from {@link NotificationDispatcher} and updates the database.
 */
@Stateless
public class MetricsCollector extends AbstractJMSMessageConsumer {

    @Inject
    private PushMessageMetricsService metricsService;

    @Resource(mappedName = "java:/queue/BatchLoadedQueue")
    private Queue batchLoadedQueue;

    @Resource(mappedName = "java:/queue/AllBatchesLoadedQueue")
    private Queue allBatchesLoaded;

    @Inject
    private Event<VariantCompleted> variantCompleted;

    @Inject
    private Event<PushMessageCompleted> pushMessageCompleted;

    /**
     * Receives metrics and update the push message information in database.
     *
     * @param variantMetricInformation the variant metrics info object
     */
    public void collectMetrics(@Observes @Dequeue VariantMetricInformation variantMetricInformation) {
        PushMessageInformation pushMessageInformation = variantMetricInformation.getPushMessageInformation();

        metricsService.updatePushMessageInformation(pushMessageInformation);

        final String variantID = variantMetricInformation.getVariantID();

        pushMessageInformation.setTotalReceivers(pushMessageInformation.getTotalReceivers() + variantMetricInformation.getReceivers());

        int loadedBatches = countLoadedBatches(variantID);
        variantMetricInformation.setServedBatches(1);
        variantMetricInformation.setTotalBatches(variantMetricInformation.getTotalBatches() + loadedBatches);

        boolean updatedExisting = false;
        for (VariantMetricInformation existingMetric : pushMessageInformation.getVariantInformations()) {
            if (variantMetricInformation.getVariantID().equals(existingMetric.getVariantID())) {
                updatedExisting = true;
                updateExistingMetric(existingMetric, variantMetricInformation);
                variantMetricInformation = existingMetric;
                break;
            }
        }

        if (!updatedExisting) {
            pushMessageInformation.addVariantInformations(variantMetricInformation);
        }

        metricsService.updatePushMessageInformation(pushMessageInformation);

        if (variantMetricInformation.getTotalBatches() == variantMetricInformation.getServedBatches()) {
            if (areAllBatchesLoaded(variantID)) {
                pushMessageInformation.setServedVariants(pushMessageInformation.getServedVariants() + 1);
                variantCompleted.fire(new VariantCompleted(pushMessageInformation.getId(), variantMetricInformation.getVariantID()));

                if (pushMessageInformation.getServedVariants() == pushMessageInformation.getTotalVariants()) {
                    pushMessageCompleted.fire(new PushMessageCompleted(pushMessageInformation.getId()));
                }
            }
        }
    }

    private int countLoadedBatches(String variantID) {
        int loadedBatches = 0;
        while (receiveNoWait(batchLoadedQueue, "variantID", variantID) != null) {
            loadedBatches += 1;
        }
        return loadedBatches;
    }

    private boolean areAllBatchesLoaded(String variantID) {
        return receiveNoWait(allBatchesLoaded, "variantID", variantID) != null;
    }

    private void updateExistingMetric(VariantMetricInformation existing, VariantMetricInformation update) {
        existing.setReceivers(existing.getReceivers() + update.getReceivers());
        existing.setServedBatches(existing.getServedBatches() + update.getServedBatches());
        existing.setTotalBatches(existing.getTotalBatches() + update.getTotalBatches());
        if (existing.getDeliveryStatus() == null) {
            existing.setDeliveryStatus(update.getDeliveryStatus());
        }
        if (existing.getDeliveryStatus() == Boolean.TRUE && update.getDeliveryStatus() == Boolean.FALSE) {
            existing.setDeliveryStatus(Boolean.FALSE);
        }
        if (existing.getReason() == null && update.getReason() != null) {
            existing.setReason(update.getReason());
        }
    }
}