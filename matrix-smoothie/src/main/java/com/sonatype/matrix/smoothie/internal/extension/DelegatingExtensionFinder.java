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
import com.google.inject.name.Names;
import com.sonatype.matrix.smoothie.Smoothie;
import hudson.Extension;
import hudson.ExtensionComponent;
import hudson.ExtensionFinder;
import hudson.model.Hudson;

import java.util.Collection;

/**
 * Delegates to {@link ExtensionFinder} implementation bound in Guice context.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.2
 */
@Extension
public class DelegatingExtensionFinder
    extends ExtensionFinder
{
    private final ExtensionFinder delegate;

    public DelegatingExtensionFinder() {
        this.delegate = Smoothie.getContainer().get(Key.get(ExtensionFinder.class, Names.named("default")));
    }

    public ExtensionFinder getDelegate() {
        return delegate;
    }

    @Override
    public <T> Collection<ExtensionComponent<T>> find(final Class<T> type, final Hudson hudson) {
        return getDelegate().find(type, hudson);
    }
}