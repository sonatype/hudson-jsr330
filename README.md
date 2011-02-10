# Description

Smoothie: Hudson JSR330 integration for Hudson

Smoothie (_a blend of guice for hudson_) was designed to allow plugin components written with **JSR-330** annotations
to be used as Hudson extension points while leaving existing `@Extension` mechanism AS-IS.

Aside from installing the custom `PluginStrategy` (and its dependencies) into the `hudson.war` 
there are no changes to `hudson-core` required.

## Building

### Requirements

* [Maven](http://maven.apache.org) 3.0+
* [Java](http://java.sun.com/) 6+ (1.6.0_19+)

Check-out and build:

    git clone git@github.com:sonatype/matrix-smoothie.git
    cd matrix-smoothie
    mvn install

## Installing

An example of a modified `hudson.war` is generated in the `matrix-smoothie-webapp` module.

### Install dependencies

Several jars need to be added to the `hudson.war`:

    Copy matrix-smoothie/target/matrix-smoothie-*.jar into hudson.war/WEB-INF/lib
    Copy matrix-smoothie-webapp/target/dependency/* into into hudson.war/WEB-INF/lib

### Configure Hudson to use Smoothie

Configure system properties:

    hudson.PluginStrategy=com.sonatype.matrix.smoothie.internal.plugin.DelegatingPluginStrategy

## Usage

### Use JSR-330 annotations to mark components

A simple component:

    @Named
    @Singleton
    public class MyPageDecorator extends PageDecorator {
        // ...
    }

Descriptors (and some other extensions) require additional `@Typed` meta-data:

    public class MyScm extends SCM {
        // ...

        @Named
        @Singleton
        @Typed(hudson.model.Descriptor.class)
        public static final class DescriptorImpl extends SCMDescriptor<MyScm> {
            // ...
        }
    }

## Advanced Usage

### Configure Aspect for Plugin development

To enable the injection of `hudson.model.Describable` instances,
configure your HPI plugin to weave the `matrix-smoothie` aspect:

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>aspectj-maven-plugin</artifactId>
                <version>1.3</version>
                <dependencies>
                    <dependency>
                        <groupId>org.aspectj</groupId>
                        <artifactId>aspectjtools</artifactId>
                        <version>1.6.10</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>test-compile</goal>
                        </goals>
                        <configuration>
                            <Xlint>ignore</Xlint>
                            <XaddSerialVersionUID>true</XaddSerialVersionUID>
                            <source>1.6</source>
                            <target>1.6</target>
                            <aspectLibraries>
                                <aspectLibrary>
                                    <groupId>org.sonatype.matrix</groupId>
                                    <artifactId>matrix-smoothie</artifactId>
                                    <version>1.1-SNAPSHOT</version>
                                </aspectLibrary>
                            </aspectLibraries>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

This will enable **setter** injection on Describable instances:

    public class MyScm extends SCM {
        // ...
        
        private transient MyScmBacking backing;
        
        @Inject
        public void setMyScmBacking(MyScmBacking backing) {
            this.backing = backing;
        }
        
        @Named
        @Singleton
        @Typed(hudson.model.Descriptor.class)
        public static final class DescriptorImpl extends SCMDescriptor<MyScm> {
            // ...
        }
    }

Remember to make fields holding onto injected resources in Describable instances **transient**.
When the object is deserialized Smoothie will re-inject the instance.

Constructor injection is **NOT** available for Describable instances.

## Trying it out

    java -DHUDSON_HOME=target/hudson \
        -Dhudson.PluginStrategy=com.sonatype.matrix.smoothie.internal.plugin.DelegatingPluginStrategy \
        -jar matrix-smoothie-webapp/target/smoothie-hudson.war

This should add a new smoothie-enabled management link and a new smoothie builder, both to simply say hello
by delegation to an injected component.
