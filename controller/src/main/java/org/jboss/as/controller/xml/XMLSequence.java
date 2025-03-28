/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.Assert;

/**
 * Encapsulates a group of XML particles with xs:sequence (i.e. ordered) semantics.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLSequence<RC, WC> extends XMLParticleGroup<RC, WC> {

    interface Builder<RC, WC> extends XMLParticleGroup.Builder<RC, WC, XMLElement<RC, WC>, XMLSequence<RC, WC>, Builder<RC, WC>> {
    }

    class DefaultBuilder<RC, WC> extends XMLParticleGroup.AbstractBuilder<RC, WC, XMLElement<RC, WC>, XMLSequence<RC, WC>, Builder<RC, WC>> implements Builder<RC, WC> {
        DefaultBuilder(FeatureRegistry registry) {
            super(registry);
        }

        @Override
        public XMLSequence<RC, WC> build() {
            return new DefaultXMLSequence<>(this.getGroups(), this.getCardinality());
        }

        @Override
        protected Builder<RC, WC> builder() {
            return this;
        }
    }

    class DefaultXMLSequence<RC, WC> extends DefaultXMLParticleGroup<RC, WC> implements XMLSequence<RC, WC> {
        private final List<QName> names;

        private static <RC, WC> Set<QName> collectNames(List<XMLParticleGroup<RC, WC>> groups) {
            Set<QName> names = new TreeSet<>(QNameResolver.COMPARATOR);
            for (XMLParticleGroup<RC, WC> group : groups) {
                names.addAll(group.getReaderNames());
                // Look no further than first required particle of sequence
                if (group.getCardinality().isRequired()) {
                    break;
                }
            }
            return Collections.unmodifiableSet(names);
        }

        protected DefaultXMLSequence(List<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            this(collectNames(groups), Collections.unmodifiableList(groups), cardinality);
        }

        private DefaultXMLSequence(Set<QName> names, List<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            super(names, groups, cardinality, new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    // Validate entry criteria
                    Assert.assertTrue(reader.isStartElement());
                    Iterator<XMLParticleGroup<RC, WC>> sequence = groups.iterator();
                    XMLParticleGroup<RC, WC> group = null;
                    int occurrences = 0;
                    do {
                        if (group != null) {
                            if (!group.getReaderNames().contains(reader.getName())) {
                                if (occurrences < group.getCardinality().getMinOccurs()) {
                                    throw ParseUtils.minOccursNotReached(reader, group.getNames(), group.getCardinality());
                                }
                                group = this.findNext(reader, context, sequence);
                                occurrences = 0;
                            }
                        } else {
                            group = this.findNext(reader, context, sequence);
                        }
                        if (group != null) {
                            occurrences += 1;
                            // Validate maxOccurs
                            if (occurrences > group.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE)) {
                                throw ParseUtils.maxOccursExceeded(reader, group.getNames(), group.getCardinality());
                            }
                            group.getReader().readElement(reader, context);
                        }
                    } while ((group != null) && (reader.getEventType() != XMLStreamConstants.END_ELEMENT));

                    // Verify that any remaining groups in sequence are optional
                    while (sequence.hasNext()) {
                        group = sequence.next();
                        if (group.getCardinality().isRequired()) {
                            throw ParseUtils.minOccursNotReached(reader, group.getNames(), group.getCardinality());
                        }
                        group.getReader().whenAbsent(context);
                    }
                }

                @Override
                public void whenAbsent(RC context) {
                    for (XMLParticleGroup<RC, WC> group : groups) {
                        group.getReader().whenAbsent(context);
                    }
                }

                private XMLParticleGroup<RC, WC> findNext(XMLExtendedStreamReader reader, RC context, Iterator<XMLParticleGroup<RC, WC>> remaining) throws XMLStreamException {
                    while (remaining.hasNext()) {
                        XMLParticleGroup<RC, WC> group = remaining.next();
                        if (group.getReaderNames().contains(reader.getName())) {
                            return group;
                        }
                        // Validate minOccurs
                        if (group.getCardinality().isRequired()) {
                            throw ParseUtils.minOccursNotReached(reader, group.getNames(), group.getCardinality());
                        }
                        group.getReader().whenAbsent(context);
                    }
                    return null;
                }
            });
            this.names = groups.stream().map(XMLParticleGroup::getNames).flatMap(Collection::stream).collect(Collectors.toUnmodifiableList());
        }

        @Override
        public Collection<QName> getNames() {
            return this.names;
        }
    }
}
