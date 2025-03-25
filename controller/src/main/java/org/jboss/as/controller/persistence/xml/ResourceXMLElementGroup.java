/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.persistence.xml;

import java.util.Map;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.xml.QNameResolver;
import org.jboss.as.controller.xml.XMLElementGroup;
import org.jboss.dmr.ModelNode;

/**
 * Encapsulates a group of XML elements for a subsystem resource.
 * @author Paul Ferraro
 */
public interface ResourceXMLElementGroup extends XMLElementGroup<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    interface Builder<T extends ResourceXMLElementGroup, B extends Builder<T, B>> extends XMLElementGroup.Builder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, ResourceXMLElement, T, B>, AttributeDefinitionXMLConfiguration.Configurator<B> {
        /**
         * Adds the specified attribute to this group, if it will parse and/or marshal as an xs:element.
         * @param attribute an attribute
         * @return a reference to this builder
         */
        B addElement(AttributeDefinition attribute);

        /**
         * Bulk operation that adds the specified attributes, that will parse and/or marshal as an xs:element, to this group.
         * @param attributes zero or more resource attribute definitions
         * @return a reference to this builder
         */
        B addElements(Iterable<? extends AttributeDefinition> attributes);

        /**
         * Bulk operation that provides the specified attributes, that will parse and/or marshal as an xs:element, to this group.
         * @param providers zero or more resource attribute definition providers
         * @return a reference to this builder
         */
        default B provideElements(Iterable<? extends Supplier<AttributeDefinition>> providers) {
            return this.addElements(new MappedIterable<>(providers, Supplier::get));
        }
    }

    abstract class AbstractBuilder<T extends ResourceXMLElementGroup, B extends Builder<T, B>> extends XMLElementGroup.AbstractBuilder<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode, ResourceXMLElement, T, B> implements Builder<T, B> {
        private final QNameResolver resolver;
        private volatile AttributeDefinitionXMLConfiguration configuration;

        AbstractBuilder(FeatureRegistry registry, QNameResolver resolver, AttributeDefinitionXMLConfiguration configuration) {
            super(registry);
            this.resolver = resolver;
            this.configuration = configuration;
        }

        @Override
        public B addElement(AttributeDefinition attribute) {
            if (this.enables(attribute)) {
                QName name = this.configuration.getName(attribute);
                AttributeParser parser = this.configuration.getParser(attribute);
                AttributeMarshaller marshaller = this.configuration.getMarshaller(attribute);
                if (parser.isParseAsElement() || marshaller.isMarshallableAsElement()) {
                    this.addElement(new AttributeDefinitionXMLElement.DefaultBuilder(attribute, this.resolver).withName(name).withParser(parser).withMarshaller(marshaller).build());
                }
            }
            return this.builder();
        }

        @Override
        public B addElements(Iterable<? extends AttributeDefinition> attributes) {
            for (AttributeDefinition attribute : attributes) {
                this.addElement(attribute);
            }
            return this.builder();
        }

        @Override
        public B withLocalNames(Map<AttributeDefinition, String> localNames) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public QName getName(AttributeDefinition attribute) {
                    String localName = localNames.get(attribute);
                    return (localName != null) ? this.resolve(localName) : super.getName(attribute);
                }
            };
            return this.builder();
        }

        @Override
        public B withNames(Map<AttributeDefinition, QName> names) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public QName getName(AttributeDefinition attribute) {
                    QName name = names.get(attribute);
                    return (name != null) ? name : super.getName(attribute);
                }
            };
            return this.builder();
        }


        @Override
        public B withParsers(Map<AttributeDefinition, AttributeParser> parsers) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public AttributeParser getParser(AttributeDefinition attribute) {
                    AttributeParser parser = parsers.get(attribute);
                    return (parser != null) ? parser : super.getParser(attribute);
                }
            };
            return this.builder();
        }

        @Override
        public B withMarshallers(Map<AttributeDefinition, AttributeMarshaller> marshallers) {
            this.configuration = new AttributeDefinitionXMLConfiguration.DefaultAttributeDefinitionXMLConfiguration(this.configuration) {
                @Override
                public AttributeMarshaller getMarshaller(AttributeDefinition attribute) {
                    AttributeMarshaller marshaller = marshallers.get(attribute);
                    return (marshaller != null) ? marshaller : super.getMarshaller(attribute);
                }
            };
            return this.builder();
        }
    }
}
