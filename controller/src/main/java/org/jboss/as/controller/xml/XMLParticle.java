/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.xml;

import org.jboss.as.controller.Feature;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.version.Stability;

/**
 * Encapsulates an XML particle.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLParticle<RC, WC> extends Feature {

    /**
     * Returns the cardinality of this XML particle.
     * @return the cardinality of this XML particle.
     */
    XMLCardinality getCardinality();

    /**
     * Returns the reader of this XML particle.
     * @return the reader of this XML particle.
     */
    XMLElementReader<RC> getReader();

    /**
     * Returns the writer of this XML particle.
     * @return the writer of this XML particle.
     */
    XMLContentWriter<WC> getWriter();

    interface Builder<RC, WC, T extends XMLParticle<RC, WC>, B extends Builder<RC, WC, T, B>> {
        /**
         * Override the cardinality of this XML particle
         * @param cardinality the cardinality of this XML particle
         * @return a reference to this builder
         */
        B withCardinality(XMLCardinality cardinality);

        /**
         * Builds this XML particle.
         * @return
         */
        T build();
    }

    abstract class AbstractBuilder<RC, WC, T extends XMLParticle<RC, WC>, B extends Builder<RC, WC, T, B>> implements Builder<RC, WC, T, B> {
        private volatile XMLCardinality cardinality = XMLCardinality.Single.REQUIRED;

        @Override
        public B withCardinality(XMLCardinality cardinality) {
            // minOccurs may not be negative
            // minOccurs may not exceed maxOccurs
            if ((cardinality.getMinOccurs() < 0) || (cardinality.getMinOccurs() > cardinality.getMaxOccurs().orElse(Integer.MAX_VALUE))) {
                throw ControllerLogger.ROOT_LOGGER.illegalXMLCardinality(cardinality);
            }
            this.cardinality = cardinality;
            return this.builder();
        }

        protected XMLCardinality getCardinality() {
            return this.cardinality;
        }

        protected abstract B builder();
    }

    class DefaultXMLParticle<RC, WC> implements XMLParticle<RC, WC> {
        private final XMLElementReader<RC> reader;
        private final XMLContentWriter<WC> writer;
        private final XMLCardinality cardinality;
        private final Stability stability;

        protected DefaultXMLParticle(XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer, Stability stability) {
            this.cardinality = cardinality;
            this.reader = reader;
            this.writer = writer;
            this.stability = stability;
        }

        @Override
        public XMLCardinality getCardinality() {
            return this.cardinality;
        }

        @Override
        public XMLElementReader<RC> getReader() {
            return this.reader;
        }

        @Override
        public XMLContentWriter<WC> getWriter() {
            return this.writer;
        }

        @Override
        public Stability getStability() {
            return this.stability;
        }
    }
}
