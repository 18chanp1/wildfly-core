/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.persistence.xml;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.xml.QNameResolver;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.as.controller.xml.XMLContentWriter;
import org.jboss.as.controller.xml.XMLElementReader;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Encapsulates an XML element for an {@link AttributeDefinition}.
 * @author Paul Ferraro
 */
public interface AttributeDefinitionXMLElement extends ResourceXMLElement {

    interface Builder {
        /**
         * Overrides the local name of this attribute.
         * @param localName the local name of this attribute
         * @return a reference to this builder
         */
        Builder withLocalName(String localName);

        /**
         * Overrides the qualified name of this attribute.
         * @param name the qualified name of this attribute
         * @return a reference to this builder
         */
        Builder withName(QName name);

        /**
         * Overrides the default parser of this attribute.
         * @param parser an alternate parser
         * @return a reference to this builder
         */
        Builder withParser(AttributeParser parser);

        /**
         * Overrides the default marshaller of this attribute.
         * @param marshaller an alternate marshaller
         * @return a reference to this builder
         */
        Builder withMarshaller(AttributeMarshaller parser);

        /**
         * Builds this element.
         * @return an XML element
         */
        AttributeDefinitionXMLElement build();
    }

    class DefaultBuilder implements Builder {
        private final AttributeDefinition attribute;
        private final QNameResolver resolver;
        private volatile QName name = null;
        private volatile AttributeParser parser;
        private volatile AttributeMarshaller marshaller;

        DefaultBuilder(AttributeDefinition attribute, QNameResolver resolver) {
            this.attribute = attribute;
            this.resolver = resolver;
            this.parser = attribute.getParser();
            this.marshaller = attribute.getMarshaller();
        }

        @Override
        public Builder withLocalName(String localName) {
            return this.withName(this.resolver.resolve(localName));
        }

        @Override
        public Builder withName(QName name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder withParser(AttributeParser parser) {
            this.parser = parser;
            return this;
        }

        @Override
        public Builder withMarshaller(AttributeMarshaller marshaller) {
            this.marshaller = marshaller;
            return this;
        }

        @Override
        public AttributeDefinitionXMLElement build() {
            AttributeDefinition attribute = this.attribute;
            AttributeParser parser = this.parser;
            AttributeMarshaller marshaller = this.marshaller;
            QName name = (this.name != null) ? this.name : this.resolver.resolve(parser.getXmlName(attribute));
            XMLCardinality cardinality = parser.getCardinality(attribute);
            XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader = new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
                    PathAddress operationKey = context.getKey();
                    Map<PathAddress, ModelNode> operations = context.getValue();
                    ModelNode operation = operations.get(operationKey);

                    if (parser.isParseAsElement()) {
                        parser.parseElement(attribute, reader, operation);
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            };
            XMLContentWriter<ModelNode> writer = new XMLContentWriter<>() {
                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
                    if (!this.isEmpty(model)) {
                        marshaller.marshallAsElement(attribute, model, true, writer);
                    }
                }

                @Override
                public boolean isEmpty(ModelNode model) {
                    return !marshaller.isMarshallableAsElement() || !model.hasDefined(attribute.getName());
                }
            };
            return new DefaultAttributeDefinitionXMLElement(name, cardinality, reader, writer, attribute.getStability());
        }
    }

    class DefaultAttributeDefinitionXMLElement extends DefaultResourceXMLElement implements AttributeDefinitionXMLElement {

        DefaultAttributeDefinitionXMLElement(QName name, XMLCardinality cardinality, XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader, XMLContentWriter<ModelNode> writer, Stability stability) {
            super(name, cardinality, reader, writer, stability);
        }
    }
}
