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
package org.jboss.aerogear.unifiedpush.message.jms;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.aerogear.unifiedpush.api.VariantMetricInformation;

/**
 * Consumes {@link VariantMetricInformation} from queue and pass them as a CDI event for further processing.
 *
 * This class serves as mediator for decoupling of JMS subsystem and services that observes these messages.
 */
@MessageDriven(name = "MetricsConsumer", activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/MetricsQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class VariantMetricInformationConsumer extends AbstractJMSMessageListener<VariantMetricInformation> {

    @Inject
    @Dequeue
    private Event<VariantMetricInformation> dequeueEvent;

    @Override
    public void onMessage(VariantMetricInformation message) {
        dequeueEvent.fire(message);
    }
}
