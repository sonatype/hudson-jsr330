/**
 * The MIT License
 *
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.sonatype.matrix.smoothie.internal.extension;

import com.google.inject.Key;
import com.sonatype.matrix.smoothie.SmoothieContainer;
import hudson.ExtensionComponent;
import hudson.ExtensionFinder;
import hudson.model.Hudson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.guice.bean.locators.QualifiedBean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Smoothie {@link ExtensionFinder}.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.2
 */
@Named
@Singleton
public class SmoothieExtensionFinder
    extends ExtensionFinder
{
    private static final Logger log = LoggerFactory.getLogger(SmoothieExtensionFinder.class);

    private final SmoothieContainer container;

    @Inject
    public SmoothieExtensionFinder(final SmoothieContainer container) {
        assert container != null;
        this.container = container;
    }

    /**
     * Look up extension type lists by asking the container for types with any {@link javax.inject.Qualifier} adorned annotation.
     */
    @Override
    public <T> Collection<ExtensionComponent<T>> find(final Class<T> type, final Hudson hudson) {
        assert type != null;

        if (log.isTraceEnabled()) {
            log.trace("Finding extensions: {}", type.getName());
        }

        List<ExtensionComponent<T>> components = new ArrayList<ExtensionComponent<T>>();
        try {
            Iterable<QualifiedBean<Annotation,T>> items = container.locate(Key.get(type));
            for (QualifiedBean<Annotation,T> item : items) {
                // Use our container for extendability and logging simplicity.
                SmoothieComponent<T> component = new SmoothieComponent<T>(item);
                log.trace("Found: {}", component);
                components.add(component);
            }

            if (log.isErrorEnabled()) {
                log.debug("Found {} {} components", components.size(), type.getName());
            }
        }
        catch (Exception e) {
            log.error("Extension discovery failed", e);
        }

        return components;
    }
}