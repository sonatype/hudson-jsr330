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

package com.sonatype.matrix.smoothie.internal;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.guice.bean.binders.SpaceModule;
import org.sonatype.guice.bean.reflect.ClassSpace;
import org.sonatype.guice.bean.reflect.URLClassSpace;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper module to install a {@link SpaceModule} with a {@link URLClassSpace} module
 * to scan classes for binding annotations.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 1.1
 */
public class ScanpathModule
    extends AbstractModule
{
    private static final Logger log = LoggerFactory.getLogger(ScanpathModule.class);

    private final ClassLoader parent;

    private final URL[] urls;

    public ScanpathModule(final ClassLoader parent, final URL[] urls) {
        assert parent != null;
        this.parent = parent;
        assert urls != null;
        this.urls = urls;
    }

    public ScanpathModule(final ClassLoader parent, final Class... types) {
        this(parent, scanTypes(types));
    }

    private static URL[] scanTypes(final Class[] types) {
        assert types != null;
        List<URL> path = new ArrayList<URL>();
        for (Class type : types) {
            path.add(type.getProtectionDomain().getCodeSource().getLocation());
        }
        return path.toArray(new URL[path.size()]);
    }

    @Override
    protected void configure() {
        install(new SpaceModule(createClassSpace()));
    }

    protected ClassSpace createClassSpace() {
        if (log.isDebugEnabled()) {
            log.debug("Path:");
            for (URL url : urls) {
                log.debug("  {}", url);
            }
        }

        return new URLClassSpace(parent, urls);
    }
}