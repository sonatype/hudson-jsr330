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

package com.sonatype.matrix.smoothie.internal.plugin;

import com.sonatype.matrix.smoothie.SmoothieContainer;
import hudson.Plugin;
import hudson.PluginStrategy;
import hudson.PluginWrapper;
import hudson.model.Hudson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Smoothie {@link PluginStrategy}.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.2
 */
@Named
@Singleton
public class SmoothiePluginStrategy
    implements PluginStrategy
{
    private static final Logger log = LoggerFactory.getLogger(SmoothiePluginStrategy.class);

    private final SmoothieContainer container;

    private final PluginWrapperFactory pluginFactory;

    @Inject
    public SmoothiePluginStrategy(final SmoothieContainer container, final PluginWrapperFactory pluginFactory) {
        assert container != null;
        this.container = container;
        assert pluginFactory != null;
        this.pluginFactory = pluginFactory;
    }

    private String basename(String name) {
        assert name != null;
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        int i = name.lastIndexOf("/");
        if (i != -1) {
            return name.substring(i + 1, name.length());
        }
        return name;
    }

    /**
     * Load the plugins wrapper and inject it with the {@link com.sonatype.matrix.smoothie.SmoothieContainer}.
     */
    public PluginWrapper createPluginWrapper(final File archive) throws IOException {
        assert archive != null;

        PluginWrapper plugin;
        try {
            plugin = pluginFactory.create(archive);
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        if (log.isDebugEnabled()) {
            logPluginDetails(plugin);
        }

        return plugin;
    }

    private void logPluginDetails(final PluginWrapper plugin) {
        assert plugin != null;

        log.debug("Loaded plugin: {} ({})", plugin.getShortName(), plugin.getVersion());

        // Some details are not valid until the createPluginWrapper() has returned... like bundled status

        log.debug("  State: active={}, enabled={}, pinned={}, downgradable={}", new Object[] {
            plugin.isActive(),
            plugin.isEnabled(),
            plugin.isPinned(),
            plugin.isDowngradable()
        });

        // Spit out some debug/trace details about the classpath
        PluginClassLoader cl = (PluginClassLoader)plugin.classLoader;
        URL[] classpath = cl.getURLs();
        if (classpath.length > 1) {
            log.debug("  Classpath:");
            int i=0;
            boolean trace = log.isTraceEnabled();
            for (URL url : classpath) {
                // skip the classes/ dir its always there
                if (i++ == 0) {
                    continue;
                }
                // for trace still log as debug, but flip on the full URL
                log.debug("    {}", trace ? url.toString() : basename(url.getFile()));
            }
        }

        // Spit out some debug information about the plugin dependencies
        List/*<PluginWrapper.Dependency>*/ dependencies = plugin.getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            log.debug("  Dependencies:");
            for (Object/*PluginWrapper.Dependency*/ dependency : dependencies) {
                log.debug("    {}", dependency);
            }
        }

        dependencies = plugin.getOptionalDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            log.debug("  Optional dependencies:");
            for (Object/*PluginWrapper.Dependency*/ dependency : plugin.getOptionalDependencies()) {
                log.debug("    {}", dependency);
            }
        }
    }

    /**
     * Loads the optional {@link hudson.Plugin} instance, configures and starts it.
     */
    public void load(final PluginWrapper plugin) throws IOException {
        assert plugin != null;

        if (log.isDebugEnabled()) {
            log.debug("Configuring plugin: {}", plugin.getShortName());
        }

        container.register(plugin);

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(plugin.classLoader);
        try {
            Plugin instance;

            // Load the plugin instance, if one has been configured.
            if (plugin.getPluginClass() == null) {
                instance = new Plugin.DummyImpl();
            }
            else {
                try {
                    // Ask the container to construct the instance
                    Class<? extends Plugin> type = loadPluginClass(plugin);
                    instance = container.injector(plugin).getInstance(type);
                    log.trace("Plugin instance: {}", instance);
                }
                catch (Throwable e) {
                    throw new IOException("Failed to load plugin instance for: " + plugin.getShortName(), e);
                }
            }

            plugin.setPlugin(instance);

            try {
                start(plugin);
            }
            catch (Exception e) {
                throw new IOException("Failed to start plugin: " + plugin.getShortName(), e);
            }
        }
        finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    /**
     * Isolates cast compiler warning.
     */
    @SuppressWarnings({"unchecked"})
    private Class<? extends Plugin> loadPluginClass(final PluginWrapper plugin) throws ClassNotFoundException {
        assert plugin != null;
        return (Class<? extends Plugin>) plugin.classLoader.loadClass(plugin.getPluginClass());
    }

    /**
     * Configures and starts the {@link hudson.Plugin} instance.
     */
    private void start(final PluginWrapper plugin) throws Exception {
        assert plugin != null;

        if (log.isDebugEnabled()) {
            log.debug("Starting plugin: {}", plugin.getShortName());
        }

        Plugin instance = plugin.getPlugin();
        instance.setServletContext(Hudson.getInstance().servletContext);
        instance.start();
    }

    /**
     * This method of the PluginStrategy interface is completely unused.
     */
    public void initializeComponents(final PluginWrapper plugin) {
        assert plugin != null;
        throw new Error("Unused operation");
    }
}