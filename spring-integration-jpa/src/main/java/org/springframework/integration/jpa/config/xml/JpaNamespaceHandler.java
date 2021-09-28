/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jpa.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for the JPA namespace.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class JpaNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	public void init() {
		this.registerBeanDefinitionParser("inbound-channel-adapter",  new JpaInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new JpaOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("updating-outbound-gateway", new UpdatingJpaOutboundGatewayParser());
		this.registerBeanDefinitionParser("retrieving-outbound-gateway", new RetrievingJpaOutboundGatewayParser());
	}
}
