/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.persistence.xml;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.xml.QNameResolver;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.as.controller.xml.XMLParticleGroup;
import org.jboss.as.controller.xml.XMLSequence;
import org.jboss.dmr.ModelNode;

/**
 * Encapsulates a group of XML particles for a subsystem resource using xs:sequence (i.e. ordered) semantics.
 * @author Paul Ferraro
 */
public interface ResourceXMLSequence extends ResourceXMLParticleGroup, XMLSequence<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder extends ResourceXMLParticleGroup.Builder<ResourceXMLSequence, Builder> {
    }

    class DefaultBuilder extends ResourceXMLParticleGroup.AbstractBuilder<ResourceXMLSequence, Builder> implements Builder {

        DefaultBuilder(FeatureRegistry registry, QNameResolver resolver) {
            this(registry, resolver, AttributeDefinitionXMLConfiguration.of(resolver));
        }

        DefaultBuilder(FeatureRegistry registry, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            super(registry, resolver, configuration);
        }

        @Override
        public ResourceXMLSequence build() {
            return new DefaultResourceXMLSequence(this.getGroups(), this.getCardinality());
        }

        @Override
        protected Builder builder() {
            return this;
        }
    }

    class DefaultResourceXMLSequence extends DefaultXMLSequence<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> implements ResourceXMLSequence {

        DefaultResourceXMLSequence(List<XMLParticleGroup<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode>> groups, XMLCardinality cardinality) {
            super(groups, cardinality);
        }
    }
}
