Dynamic Dependency Injection Framework
======================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.hjohn.ddif/ddif-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hjohn.ddif/ddif-core)
[![Build Status](https://github.com/hjohn/hs.ddif/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/hjohn/hs.ddif/actions)
[![Coverage](https://codecov.io/gh/hjohn/hs.ddif/branch/master/graph/badge.svg?token=QCNNRFYF98)](https://codecov.io/gh/hjohn/hs.ddif)
[![License](https://img.shields.io/badge/License-BSD_2--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)

A light-weight framework that allows you to use standard JSR-330 javax.inject
Annotations to create instances of objects, even when they're dynamically loaded at
runtime.  This framework will allow you to package your classes in separate JAR's,
load them at runtime, and have them injected with dependencies or serve as dependencies
for other classes.

For example given a class implementing the `PaymentProcessor` interface:

    public class CreditCardPaymentProcessor implements PaymentProcessor {
    }

The below class would get its `paymentProvider` field injected with an instance of
the above class:

    public class BookShop {
        @Inject
        private PaymentProcessor paymentProcessor;
    }

This framework differs from most standard DI frameworks in that dependencies can be
added and removed at runtime.  The used Injector will make sure that dependencies can
be satisfied and will refuse to add or remove classes when it would result in broken
dependencies.

For example, if a class registered with an Injector instance needs a `PaymentProvider`,
then attempting to remove the only matching `PaymentProvider` from the Injector will
fail.  Likewise, registering a class that needs a `PaymentProvider` would fail when no
class is registered that could provide one.

Other than those restrictions, the Injector is free to be modified at runtime in any
way that is desired.

You may ask if dependencies are enforced in such a way, how would it ever be possible
to depend on something that a dynamically loaded class offers?  This can be achieved by
using optional dependencies and by using Collection dependencies.

An example of a Collection dependency:

    public class BookShop {
        @Inject
        private Set<Book> booksOnShelf;
    }

If there are no books registered with the Injector, it will simply inject an
empty collection.  However, if at creation time of the BookShop instance there are
books registered those will all get injected.

To make this dependency even more dynamic, it can be wrapped in a `Provider` (see
JSR-330 docs), like this:

    public class BookShop {
        @Inject
        private Provider<Set<Book>> booksOnShelfProvider;
    }

In this instance, each time the `get` method of the Provider is called, the Injector
will look for all registered books, even ones that were added or removed after the
`BookShop` instance was created, and return them.

## Features

* Supports all JSR-330 annotations
* Light-weight, very few dependencies
* Inject classes with dependencies, even if loaded at runtime
  * Field and constructor injection
  * Assisted injection with the `@Producer` and `@Parameter` annotations
  * Optional injection with the `@Opt` annotation or any `@Nullable` annotation
  * Scoping of injectables with the `ScopeResolver` interface
  * Injection of generic types
  * Injection of collections containing all matching dependencies, if any
* Injection candidates can be supplied by:
  * Scanning packages or jars for annotated classes using `ComponentScanner`
  * Auto discovery of dependencies not explicitly registered
  * Producer methods or fields annotated with the `@Produces` annotation
  * JSR-330 providers 

## Requirements
* Java Runtime Environment 8+ installed

# Getting started
A basic example will be used to illustrate how this framework works.  We start with
a class that needs some dependencies injected:

    public class BookShop {

        @Inject
        private Set<Book> booksOnShelf;

        @Inject
        public BookShop(CreditCardPaymentProcessor paymentProcessor) {
            ...
        }
    }

To have this class injected, we'll need to create an Injector and configure it
properly:

    Injector injector = new Injector();

    injector.register(CreditCardPaymentProcessor.class);
    injector.register(BookShop.class);

Now we can ask the Injector to create us an instance of `BookShop`:

    BookShop bookShop = injector.getInstance(BookShop.class);

This will create a `BookShop` instance by calling the constructor annotated with `@Inject` and providing it with
an instance of `CreditCardPaymentProcessor`.  The `booksOnShelf` field will be injected (after the Constructor
has completed) with an empty Set as no `Book` injection candidates are currently known to the Injector.

By registering some `Book` objects, and asking for a new Instance of `BookShop` we can get the `booksOnShelf` field
populated with some books:

    injector.registerInstance(new Book("Dune"));

    BookShop anotherBookShop = injector.getInstance(BookShop.class);

The second `BookShop` instance will have a `Set` of `Book` objects matching those known by the Injector.  If you
wanted to have all `BookShop` instances to have access to the latest known books, you can wrap the collection
with a `Provider`.  The `BookShop` instances can then query for the latest books by calling the `get` method of
the `Provider` any time they want:

    public class BookShop {

        @Inject
        private Provider<Set<Book>> booksOnShelf;

        public Set<Book> getLatestBooks() {
            return booksOnShelf.get();
        }

        ...
    }

## Auto discovery of dependencies

Registering every dependency manually (and in the correct order) can quickly become tedious.  In our above
example, the `BookShop` class needed to be registered as well as its `CreditCardPaymentProcessor` dependency.
We can however have the Injector discover these automatically, as long as the dependencies themselves are
concrete classes that have a public default constructor or have exactly one constructor with annotated with `@Inject`.

In order to discover dependencies automatically, we have to create the Injector slightly differently:

    Injector injector = new Injector(true);

Now we can get an instance of `BookShop` without any further dependencies needing to be registered:

    BookShop bookShop = injector.getInstance(BookShop.class);

Under the hood, the Injector will notice there is no `BookShop` injection candidate registered.  However,
by analyzing the `BookShop` class it sees that it can be instantiated with a Constructor that requires a
`CreditCardPaymentProcessor` -- unfortunately, there is also no `CreditCardPaymentProcessor` registered.  The
Injector then recursively analyzes the `CreditCardPaymentProcessor` class, and registers this class with
itself as it noticed that it can be simply instantiated with a default constructor.

Now that the dependencies for `BookShop` can also be satisfied, the `BookShop` class is registered with the 
Injector, and an instance is returned.  

## Assisted Injection

Assisted injection makes it possible to automatically create a factory for a class that will inject known
dependencies automatically while allowing you to supply additional parameters of your own. To indicate that
a specific class needs a factory annotate it with `@Producer` and indicate which injections it requires
should be parameters:

    @Producer(CarFactory.class)
    public class Car {
        @Inject
        public Car(Engine engine, @Parameter int wheelCount) { ... }
    }

Next create an interface with a single method with only the parameters you wish to supply.  The injector
will automatically implement it:

    interface CarFactory {
        Car createCar(int wheelCount);
    }

The interface can now be injected anywhere you wish to create a `Car` instance:

    public class Garage {
        @Inject
        public Garage(CarFactory factory) {
            factory.createCar(5);
        }
    }

## Producers
Producers are fields or methods that can supply a dependency that can be used for injection. They are useful
for objects that are too complex for the injector to construct itself or for example make use of object pooling.

For example:

    @Producer
    public static Connection createConnection() {
        return new Connection( ... );
    }

When registering a class containing this method, the injector will start providing instances of `Connection`
by calling the `createConnection` method every time a `Connection` is needed.

It is also possible to annotate a field in this way:

    @Producer 
    private static Connection connection = new Connection();

Just like the method, the field will be read every time a new `Connection` is required.

### Scoping

By default, an instance produced by a producer has no scope applied to it. This means it will be called or 
read every time an instance of the type it supplies is required. By annotating the producer method or field
with a scope annotation the same instance will be returned by the injector within an active scope. For example:

    @Producer
    @Singleton
    public static Connection createConnection() {
        return new Connection( ... );
    }

The above producer will only be called once, and the same `Connection` instance will be injected everywhere.

### Qualifiers

Producers may be annotated with qualifiers to distinguish instances of the same type:

    @Producer @Red Color color = Color.RED;
    @Producer @Green Color createGreen() { return Color.GREEN; }
    @Producer @Named("username") String name = "John";

At injection sites this qualifier can then be used to get a specific instance:
 
    @Inject @Red Color color;  // will be Color.RED
    @Inject @Green Color color;  // will be Color.GREEN
    @Inject @Named("username") String name;  // will be "John"

Or to get all `Color` instances:

    @Inject List<Color> colors;  // will be List.of(Color.RED, Color.GREEN)

### Dependencies

A producer can also have dependencies of its own. This can be achieved by either adding parameters to the
method declaration or by making it a non-static method of a class that has dependencies. Here are two ways to make
the `createConnection` method depend on a `Credentials` instance:

    @Producer
    public static Connection createConnection(Credentials credentials) {
        return new Connection(credentials, ... );
    }

Or:

    public class DatabaseSetup {
        @Inject Credentials credentials;

        @Producer
        public Connection createConnection() { 
            return new Connection(credentials, ... );
        }
    }

Note that for non-static producer methods or fields, an instance of the declaring class must be available or
created on demand. It is not possible for the declaring class to be directly or indirectly dependent on
something one of its fields or methods produces as this would be a circular dependency. Declare the relevant
producer `static` or use providers to avoid this.

## Optional dependencies

Dependencies can be marked optional by either using a `Nullable` annotation (any annotation named `Nullable`
is supported) or the `Opt` annotation provided by this project. When such a dependency is encountered,
it will not be injected if the required dependency is not available. For example:

    @Inject @Nullable @Named("database.user") private String user;
    @Inject @Nullable @Named("database.timeout") private Integer timeout = 1000;

In the above example, `user` will be left `null` and `timeout` will be left `1000` if the corresponding
dependency is unavailable. Note that at this time primitive types are not supported as dependencies.

## Scoping

By default, all dependencies are created by the injector each time they are needed, except for dependencies
supplied by providers or producers (which gives the user control over when to create a new instance or when
to return an existing one) or dependencies which were registered as an instance.

To prevent the injector creating a new instance every time, classes and producers can be annotated with
`@Singleton`.

### Scope Resolvers

An injector can be configured with instances of the `ScopeResolver` interface. This interface makes it
possible to introduce a custom scope allowing the injector to track a different instance of a dependency
depending on the currently active scope it is accessed within.  The `AbstractScopeResolver` subclass
simplifies this process further by only requiring the name of the annotation that marks the custom
scope, and a method which identifies which scope is currently active.

For example, given a `ScopeResolver`:

    public class TestScopeResolver extends AbstractScopeResolver<String> {
        private static String currentScope = "default";

        @Override
        public Class<? extends Annotation> getScopeAnnotationClass() {
            return TestScoped.class;
        }

        @Override
        public String getCurrentScope() {
            return currentScope;
        }

        public void changeScope(String scope) {
            this.currentScope = scope;
        }
    }

.. and a `@Scope` annotation:

    @Scope
    @Documented
    @Retention(RUNTIME)
    public @interface TestScoped {  }

... and a `@TestScoped` annotated class registered with the injector:

    @TestScoped
    public class Car { }

... and an injector constructed with the above `ScopeResolver`:

    Injector injector = new Injector(new TestScopeResolver());

    injector.register(Car.class);

When asking the injector for a `Car` instance, it will first determine the current scope. This is achieved
by finding the `ScopeResolver` responsible for resolving `TestScoped` annotations. The resolver is queried
for an instance of `Car`, which the `AbstractScopeResolver` will attempt to find in the currently active scope
supplied by `getCurrentScope`.

If no `Car` instance is available in the current scope, the injector will create one and store it in the
resolver. If one was available, it is simply returned. For example:

    Car car = injector.getInstance(Car.class);

The above will return a new `Car` and is registered with our `TestScopeResolver`. When the injector is
queried again, the same instance is returned:

    injector.getInstance(Car.class) == injector.getInstance(Car.class)

Depending on the currently active scope, the injector will return different instances. Let `resolver` be
an instance of our `TestScopeResolver`:

    Car car = injector.getInstance(Car.class);
    Car sameCar = injector.getInstance(Car.class);

    assert car == sameCar;

    resolver.changeScope("local");

    Car otherCar = injector.getInstance(Car.class);

    assert car != otherCar;

Scopes can also be inactive. The `AbstractScopeResolver` allows this by returning a `null` as current scope.
When an attempt is made to get an instance of a scoped object when no scope is active, an `OutOfScopeException`
will be thrown:

    resolver.changeScope(null);

    Car car = injector.getInstance(Car.class);  // throws exception

### `@WeakSingleton`

This is a special case of the `@Singleton` annotation. The injector will create a weak reference to these
singletons to allow garbage collection if only the injector refers to the instance. If garbage
collection occurred, and a new instance of the singleton is needed, it will be created again.

The weak singletons are primarily useful to be able to unload classes without having to destroy the injector.
When dynamically loading jars containing classes that are registered with an injector, it will be impossible
to unload these classes later if they are annotated with `@Singleton`.  Instead, `@WeakSingleton` should be
used here so that when these classes are no longer needed they can be completely unloaded.

# Open issues

* No method injection support
* Only `java.util.Set` and `java.util.List` are currently supported for collection injection
* Collection injection conflicts with injecting classes that extend a collection interface
* Many Qualifiers on a single class (10+) will probably cause issues, some kind of limit needs to be enforced to
  prevent this, or the issue needs to be addressed with a better solution

# BSD License

Copyright (c) 2013-2021, John Hendrikx
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Dependencies and Acknowledgements

### Reflections

License: WTFPL (http://www.wtfpl.net/)  
https://github.com/ronmamo/reflections

### Geantyref

License: Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0)   
https://github.com/leangen/geantyref

### Apache Commons Lang

License: Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0)  
https://commons.apache.org/proper/commons-lang/

### Byte Buddy

License: Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0)  
https://bytebuddy.net/

### DirectedGraph and TopologicalSort

by Keith Schwarz  
License: Public Domain  
http://www.keithschwarz.com/interesting/
