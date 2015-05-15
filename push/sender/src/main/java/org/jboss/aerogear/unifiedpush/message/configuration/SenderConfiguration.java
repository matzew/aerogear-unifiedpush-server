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

package org.jboss.aerogear.unifiedpush.message.configuration;

import org.jboss.aerogear.unifiedpush.api.validation.DeviceTokenValidator;
import org.jboss.aerogear.unifiedpush.message.sender.PushNotificationSender;

/**
 * Push Network specific configuration for UPS message sending mechanism.
 *
 * Allows to configure how are messages loaded and treated inside of UPS messaging system.
 *
 * Configurations for Push Networks are loaded by {@link SenderConfigurationProvider}.
 *
 * @see SenderConfigurationProvider
 */
public class SenderConfiguration {

    private int batchesToLoad;
    private int batchSize;

    public SenderConfiguration() {
    }

    public SenderConfiguration(int batchesToLoad, int batchSize) {
        this.batchesToLoad = batchesToLoad;
        this.batchSize = batchSize;
    }

    /**
     * Specifies how many batches should be loaded in one transaction.
     *
     * UPS splits device tokens loaded from database into batches and loads at most {@link #batchesToLoad()} in one transaction.
     *
     * This avoids long transactions and enables fail-over procedures.
     */
    public int batchesToLoad() {
        return batchesToLoad;
    }

    /**
     * Batch size specifies how many devices will be loaded and delivered in one batch.
     *
     * One batch is a unit of work for the final {@link PushNotificationSender} implementation.
     *
     * Smaller batches allows better fail-over, but can be unfriendly to the Push Network, since it has to allow UPS establish more connections.
     *
     * Larger batches allows for more effective communication, however you must be aware of memory limits configured for UPS message queues.
     *
     * One device token theoretically requires 2 * (upper-bound limit of its length) bytes of memory.
     * The minimum or maximum lengths of specific push networks are partially listed in {@link DeviceTokenValidator}.
     */
    public int batchSize() {
        return batchSize;
    }

    /**
     * This is a derived property, computed as a product of {@link #batchesToLoad()} and {@link #batchSize()}.
     */
    public int tokensToLoad() {
        return batchesToLoad * batchSize;
    }
}
