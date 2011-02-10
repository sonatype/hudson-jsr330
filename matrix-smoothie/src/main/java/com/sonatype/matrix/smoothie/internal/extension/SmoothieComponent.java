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

import com.sonatype.matrix.smoothie.Smoothie;
import hudson.ExtensionComponent;
import org.sonatype.guice.bean.locators.QualifiedBean;

import java.lang.annotation.Annotation;

/**
 * Smoothie component extension holder.
 *
 * This primarily exists to provide better {@link #toString} as well as to expose the {@link QualifiedBean}.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.2
 */
public class SmoothieComponent<T>
    extends ExtensionComponent<T>
{
    private final QualifiedBean<Annotation,T> item;

    public SmoothieComponent(final QualifiedBean<Annotation,T> item) {
        super(item.getValue(), Smoothie.priorityOf(item));
        this.item = item;
    }

    public Class<?> getType() {
        return getItem().getValue().getClass();
    }

    public double getPriority() {
        return ordinal();
    }

    public QualifiedBean<Annotation,T> getItem() {
        return item;
    }

    @Override
    public String toString() {
        return "SmoothieComponent{" +
            "type=" + getType().getSimpleName() +
            ", priority=" + getPriority() +
            ", item=" + item +
            '}';
    }
}