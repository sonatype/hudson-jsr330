/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.sun.com/cddl/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is SezPoz. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 2006-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package net.java.sezpoz;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.sezpoz.impl.SerAnnotatedElement;

import org.sonatype.guice.bean.reflect.ClassSpace;
import org.sonatype.guice.bean.reflect.URLClassSpace;

/**
 * Represents an index of a single annotation.
 * Indices are <em>not</em> automatically cached
 * (but reading them should be pretty cheap anyway).
 * @param T the type of annotation to load
 * @param I the type of instance which will be created
 */
public final class SpaceIndex<T extends Annotation, I> implements Iterable<SpaceIndexItem<T,I>> {

    private static final Logger LOGGER = Logger.getLogger(SpaceIndex.class.getName());

    /**
     * Load an index for a given annotation type.
     * Uses the thread's context class loader to find the index and load annotated classes.
     * @param annotation the type of annotation to find
     * @param instanceType the type of instance to be created (use {@link Void} if all instances will be null)
     * @return an index of all elements known to be annotated with it
     * @throws IllegalArgumentException if the annotation type is not marked with {@link Indexable}
     *                                  or the instance type is not equal to or a supertype of the annotation's actual {@link Indexable#type}
     */
    public static <T extends Annotation,I> SpaceIndex<T,I> load(Class<T> annotation, Class<I> instanceType) throws IllegalArgumentException {
        return load(annotation, instanceType, new URLClassSpace(Thread.currentThread().getContextClassLoader()));
    }

    /**
     * Load an index for a given annotation type.
     * @param annotation the type of annotation to find
     * @param instanceType the type of instance to be created (use {@link Void} if all instances will be null)
     * @param space a class space in which to find the index and any annotated classes
     * @return an index of all elements known to be annotated with it
     * @throws IllegalArgumentException if the annotation type is not marked with {@link Indexable}
     *                                  or the instance type is not equal to or a supertype of the annotation's actual {@link Indexable#type}
     */
    public static <T extends Annotation,I> SpaceIndex<T,I> load(Class<T> annotation, Class<I> instanceType, ClassSpace space) throws IllegalArgumentException {
        return new SpaceIndex<T,I>(annotation, instanceType, space);
    }

    private final Class<T> annotation;
    private final Class<I> instanceType;
    private final ClassSpace space;

    private SpaceIndex(Class<T> annotation, Class<I> instance, ClassSpace space) {
        this.annotation = annotation;
        this.instanceType = instance;
        this.space = space;
    }

    /**
     * Find all items in the index.
     * Calls to iterator methods may fail with {@link IndexError}
     * as the index is parsed lazily.
     * @return an iterator over items in the index
     */
    public Iterator<SpaceIndexItem<T,I>> iterator() {
        return new LazyIndexIterator();
    }

    /**
     * Lazy iterator. Opens and parses annotation streams only on demand.
     */
    private final class LazyIndexIterator implements Iterator<SpaceIndexItem<T,I>> {

        private Enumeration<URL> resources;
        private ObjectInputStream ois;
        private URL resource;
        private SpaceIndexItem<T,I> next;
        private boolean end;
        private final Set<String> loadedMembers = new HashSet<String>();

        public LazyIndexIterator() {
            if (LOGGER.isLoggable(Level.FINE)) {
                String urls;
                if (space instanceof URLClassSpace) {
                    urls = " " + Arrays.toString(((URLClassSpace) space).getURLs());
                } else {
                    urls = "";
                }
                LOGGER.log(Level.FINE, "Searching for indices of {0} in {1}{2}", new Object[] {annotation, space, urls});
            }
        }

        private void peek() throws IndexError {
            try {
                for (int iteration = 0; true; iteration++) {
                    if (iteration == 9999) {
                        LOGGER.log(Level.WARNING, "possible endless loop getting index for {0} from {1}", new Object[] {annotation, space});
                    }
                    if (next != null || end) {
                        return;
                    }
                    if (ois == null) {
                        if (resources == null) {
                            resources = space.findEntries("META-INF/annotations/", annotation.getName(), false);
                        }
                        if (!resources.hasMoreElements()) {
                            // Exhausted all streams.
                            end = true;
                            return;
                        }
                        resource = resources.nextElement();
                        LOGGER.log(Level.FINE, "Loading index from {0}", resource);
                        ois = new ObjectInputStream(resource.openStream());
                    }
                    SerAnnotatedElement el = (SerAnnotatedElement) ois.readObject();
                    if (el == null) {
                        // Skip to next stream.
                        ois.close();
                        ois = null;
                        continue;
                    }
                    String memberName = el.isMethod ? el.className + '#' + el.memberName + "()" :
                        el.memberName != null ? el.className + '#' + el.memberName :
                            el.className;
                    if (!loadedMembers.add(memberName)) {
                        // Already encountered this element, so skip it.
                        LOGGER.log(Level.FINE, "Already loaded index item {0}", el);
                        continue;
                    }
                    // XXX JRE #6865375 would make loader param accurate for duplicated modules
                    next = new SpaceIndexItem<T,I>(el, annotation, instanceType, space, resource);
                    break;
                }
            } catch (Exception x) {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException x2) {
                        LOGGER.log(Level.WARNING, null, x2);
                    }
                }
                throw new IndexError(x);
            }
        }

        public boolean hasNext() {
            peek();
            return !end;
        }

        public SpaceIndexItem<T,I> next() {
            peek();
            if (!end) {
                assert next != null;
                SpaceIndexItem<T,I> _next = next;
                next = null;
                return _next;
            } else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
