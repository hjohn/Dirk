# Dirk DI

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.int4.dirk/dirk-di/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.int4.dirk/dirk-di)
[![Build Status](https://github.com/hjohn/hs.ddif/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/hjohn/hs.ddif/actions)
[![Coverage](https://codecov.io/gh/hjohn/Dirk/branch/master/graph/badge.svg?token=QCNNRFYF98)](https://codecov.io/gh/hjohn/hs.ddif)
[![License](https://img.shields.io/badge/License-BSD_2--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![javadoc](https://javadoc.io/badge2/org.int4.dirk/parent/javadoc.svg)](https://javadoc.io/doc/org.int4.dirk/parent)

Dirk is a small, highly customizable, dynamic dependency injection framework.

## Quick Start

Add a dependency to Dirk in your POM:

    <groupId>org.int4.dirk</groupId>
    <artifactId>dirk-di<artifactId>
    <version>1.0.0-alpha3</version>

Assume there is a small class which depends on a `String` which Dirk should provide:

    public class Greeter {
        @Inject private String greeting;

        public void greet() {
            System.out.println(greeting);
        }
    }

Create an injector:

    Injector injector = Injectors.autoDiscovering();

Register a `String` and the `Greeter` class:

    injector.registerInstance("Hello World");
    injector.register(Greeter.class);

Let the injector create an instance of `Greeter`, then call its `greet` method and observe that the
injected greeting is printed to the console:

    Greeter greeter = injector.getInstance(Greeter.class);

    greeter.greet();  // prints "Hello World"

## Features

- Dependency Injection
  - Constructor, Method and Field injection
  - Supports qualifiers, scopes, generics and lifecycle callbacks
- Dynamic
  - Register and unregister types at any time
  - Ensures all dependencies are always resolvable and unambiguous
- Highly Customizable
  - Choose what annotations and extensions Dirk should use
  - Built-in styles for Jakarta (CDI), JSR-330 and Dirk DI or create a new one
- Extendable
  - Fully documented API and SPI
  - Common DI features are just extensions in Dirk
- Small
  - Core jar and its dependencies are around 200 kB

Several well known features of DI systems are implemented as standard extensions to Dirk's core system. Included
are extensions to support:

- Producer methods and fields
- Delayed lookup of dependencies (providers)
- Assisted Injection
- Proxy creation and injection
- List, Set and Optional injection

# Available Dependency Injection Styles

| |Dirk|CDI|Jakarta|JSR-330|
|---|---|---|---|---|
|Artifact|dirk-di|dirk-cdi|dirk-jakarta|dirk-jsr330|
|Standard Annotations|`jakarta.inject`|`jakarta.inject`|`jakarta.inject`|`javax.inject`|
|Additional Annotations|`org.int4.dirk.annotations`|`jakarta.enterprise.inject`|`org.int4.dirk.annotations`|`org.int4.dirk.annotations`|
|Default Annotation|`@Default`|`@Default`|-|-|
|Any Annotation|`@Any`|`@Any`|-|-|
|Optional Injection|`@Opt`|-|`@Opt`|`@Opt`|
|Producer Support|`@Produces`|`@Produces`|`@Produces`|`@Produces`|
|Assisted Injection|`@Assisted` & `@Argument`<sup>1</sup>|-|`@Assisted` & `@Argument`<sup>1</sup>|`@Assisted` & `@Argument`<sup>1</sup>|
|Indirect Injection|`Provider`|`Provider` & `Instance`|`Provider`|`Provider`|
|Collection Injection|`List` & `Set`|-|`List` & `Set`|`List` & `Set`|
|Proxy Support|Yes<sup>2</sup>|Yes<sup>2</sup>|Yes<sup>2</sup>|Yes<sup>2</sup>|

<sup>1</sup> When detected on classpath by including `org.int4.dirk.extensions:extensions-assisted`  
<sup>2</sup> When detected on classpath by including `org.int4.dirk.extensions:extensions-proxy`

# Documentation

## Terminology

|Term|Explanation|
|---|---|
|Candidate|A qualified type that could be used to satisfy a dependency|
|Dependency|A qualified type required by an inject annotated constructor, method or field|

## Dependency Injection

Dependencies are other classes or types that are required for the correct functioning of a class. A
dependency can be a class, an interface, a generic type or a primitive type. Dependency injection supplies 
these required values automatically. Dependencies can be supplied through constructor or method parameters or
by setting fields directly. 

Constructor injection:

    public class Greeter {
        @Inject
        public Greeter(String greeting) { ... }
    }

Method injection:

    public class Greeter {
        @Inject
        void setGreeting(String greeting) { ... }
    }

Field injection:

    public class Greeter {
        @Inject 
        private String greeting;
    }

Any of the above forms can have a `String` dependency injected. Note that when using method or field injection
the values are set *after* the constructor is called. Referring to these values in the constructor therefore could
result in an error, instead consider implementing this logic in an initializer method that can be called after 
injection completes. See Lifecycle Callbacks for more information.

### Type Resolution

When considering the type to inject for a dependency, the system follows standard Java rules when doing
type conversions. Any type conversion which does not require a cast is allowed, except primitive widening 
conversions. This includes boxing and unboxing conversions and compatible generic conversions. 

When a class is registered with an injector it can satisfy one or more types. The types that can
be satisfied are all implemented interfaces, its super classes and any interfaces and super classes these
implement or extend in turn. The `Greeter` class for example could be a candidate for dependencies of type 
`Greeter` and `Object`. 

The framework also does automatic boxing and unboxing conversion. For primitive types and their boxed types
this adds another possible type they can supply. An `Integer` can be used to inject an `int` or vice versa.
Other types the `Integer` class could satisfy are `Number` (its super class), `Object` (`Number`'s
super class), `Comparable<Integer>` (implemented interface) and `Serializable` (interface implemented by 
`Number`).

Example:

    injector.registerInstance(42);  // register an int with value 42

Would satisfy all the following dependencies:

    @Inject int i;
    @Inject Integer integer;
    @Inject Number number;
    @Inject Object object;
    @Inject Comparable<Integer> integerComparable;
    @Inject Comparable<? extends Number> numberComparable;

... but would not satisfy:

    @Inject long l;  // Primitive widening conversion not allowed
    @Inject Long longValue;  // Integer cannot be cast to Long
    @Inject Comparable<Number> comparable;  // Incompatible generic type

### Qualifiers

Types registered with the injector can be annotated with qualifier annotations. These annotations provide
another way to distinguish candidates besides their types. This makes it possible to distinguish between multiple
candidates that may all match a dependency where exactly one dependency is required. Qualifiers can be placed on 
candidates and on dependencies. In order for a candidate to match, it must have all the qualifiers specified on 
the dependency.

A dependency with an `@English` qualifier annotation:

    @Inject @English private String greeting;

Or as a constructor or method parameter:

    public Greeter(@English String greeting) { ... }
    public void setGreeting(@English String greeting) { ... }

Candidates can be annotated directly with qualifiers, or they can be specified during
registration (for instances).

A `Greeter` candidate with an `@English` qualifier annotation:

    @English
    public class Greeter { ... }

Registering `String` candidate instances with different qualifiers:

    injector.registerInstance("Hello World", English.class);
    injector.registerInstance("Hallo Wereld", Dutch.class);

As an example, given two `String` candidates, one annotated with `@Greeting` and `@English`, the other
annotated with `@Greeting` and `@Dutch`:

    injector.registerInstance("Hello World", English.class, Greeting.class);
    injector.registerInstance("Hallo Wereld", Dutch.class, Greeting.class);

Then the following dependencies could be satisfied:

    @Greeting @English String s;  // an English greeting
    @Greeting @Dutch String s;    // a Dutch greeting
    @English String s;            // any English String
    @Dutch String s;              // any Dutch String
    
The following dependencies will not be satisfied:

    @Greeting String s;          // ambiguous, English or Dutch greeting?
    String s;                    // ambiguous, there are two String candidates
    @Greeting @French String s;  // unsatisfiable, no French greeting was registered
    @English int englishNumber;  // unsatisfiable, no int was registered

### Scopes

Scopes are used to control the lifecycle of candidates, and which instance of a candidate is used to satisfy a
dependency. The injector supports two types of scopes, pseudo-scopes and normal scopes. Candidates which have a 
pseudo-scope are never wrapped in a proxy, and do not need to use indirection to resolve scope conflicts. Candidates 
with a normal scope will require a proxy (or indirection via a provider) when injected into other candidates with a 
different scope.

Which scopes are considered pseudo-scopes and which scopes provide the mandatory singleton and unscoped scopes is 
determined by the used `ScopeStrategy`. The actual implementation of each scope is provided by a corresponding
`ScopeResolver` when creating the injector.

### Lifecycle

When the injector is configured to do lifecycle callbacks (for example, calling  `@PostConstruct` or `@PreDestroy`
annotated methods), the injector will call these, respectively, after injection completes and just before the candidate
is removed.

## Dependency validation during registration

When adding or removing candidates from the injector, the injector ensures that all (remaining) registered 
candidates can have their dependencies satisfied. If the addition or removal would result in unsatisfied or
ambiguous dependencies then an exception is thrown explaining the problem and the addition or removal is rolled back
to the previous consistent state.

The injector can throw the following exceptions to indicate a problem during addition or removal:

- `AmbiguousDependencyException` when a new candidate was added which requires a single candidate for a dependency
but multiple candidates are available
- `UnsatisfiedDependencyException` when a new candidate was added which requires a single candidate for a dependency
but no candidates were available
- `AmbiguousRequiredDependencyException` when the addition or removal of a candidate would cause another dependency in
another candidate to become ambiguous, for example when a new candidate supplies another option for a dependency that 
was already satisfied
- `UnsatisfiedRequiredDependencyException` when the addition or removal of a candidate would cause another dependency 
in another candidate to become unsatisfied, for example when a removed candidate was the only supplier of a dependency
- `CyclicDependencyException` when a dependency cycle was detected amongst two or more candidates that was not broken
by making use of a provider
- `ScopeConflictException` when a scoped dependency depends directly on another scoped dependency, and the conflict
could not be resolved automatically by means of a proxy
  
## Registration of Candidates at runtime

Dirk allows addition and removal of new candidates at any time, assuming the change won't leave the injector in an
inconsistent state where it can't satisfy all its existing dependencies.

Let's assume there is a class which requires a list of books:

    class BookShop {
        @Inject List<Book> availableBooks;

        List<Book> getBooks() { return availableBooks; }
    }

The class is registered, and an instance is obtained:

    injector.register(BookShop.class);
    
    BookShop bookShop = injector.getInstance(BookShop.class);

When calling `getBooks` the list will be empty as no candidates were registered that supply a type `Book`:

    assertThat(bookShop.getBooks()).isEmpty();  // passes

This can be resolved by registering a `Book` and obtaining a new `BookShop` instance:

    injector.registerInstance(new Book("Dune"));

    BookShop bookShop2 = injector.getInstance(BookShop.class);

    assertThat(bookShop2.getBooks()).hasSize(1);  // passes

Creating a new `BookShop` every time the list of available books changes is a bit cumbersome, so it is possible to 
use indirection to obtain the latest available books by means of a provider. Redefine the `BookShop` class:

    class ModifiedBookShop {
        @Inject Provider<List<Book>> availableBooks;

        List<Book> getBooks() { return availableBooks.get(); }
    }

An instance of this class will now immediately respond to a change in the books available:

    injector.register(ModifiedBookShop.class);
    
    ModifiedBookShop modifiedBookShop = injector.getInstance(ModifiedBookShop.class);

    assertThat(modifiedBookShop.getBooks()).isEmpty();  // passes

    injector.registerInstance(new Book("Dune"));

    assertThat(modifiedBookShop.getBooks()).hasSize(1);  // passes

In more complex scenario's it is sometimes necessary to register multiple candidates simultaneously to avoid a chicken/egg
type problem. Given two classes:

    class A {
        @Inject B b;  // depends on B
    }

    class B {
        @Inject Provider<A> a;  // indirectly depends on A
    }
    
Registering either of these two candidates separately is not allowed as not all dependencies could be satisfied:

    injector.register(A.class);  // --> UnsatisfiedDependencyException, requires B
    injector.register(B.class);  // --> UnsatisfiedDependencyException, requires A

However, registering both at the same time works as expected:

    injector.register(List.of(A.class, B.class));  // works!

## Auto Discovery

When auto discovery is enabled, Dirk will attempt to automatically register candidates referred to in dependencies
that are not yet registered, assuming it can find a way to construct them. Given two classes:

    class BookShop {
        @Inject CreditCardPaymentProcessor paymentProcessor;
    }

    public class CreditCardPaymentProcessor {
    }

In order to create a `BookShop` instance, normally a `CreditCardPaymentProcessor` must be registered first (or simultaneously):

    Injector injector = Injectors.manual();

    injector.register(CreditCardPaymentProcessor.class);
    injector.register(BookShop.class);

    BookShop bookShop = injector.getInstance(BookShop.class);

Since `CreditCardPaymentProcessor` has an empty public constructor, the candidate could be automatically discovered when
Dirk encounters the missing dependency in `BookShop`:

    Injector injector = Injectors.autoDiscovering();

    injector.register(BookShop.class);  // registers BookShop and the discovered type CreditCardPaymentProcessor

    BookShop bookShop = injector.getInstance(BookShop.class);

This automatic discovery works recursively and so large graphs of dependent candidates can all be discovered by 
registering only a few root candidates.

## Optional Injection

Dirk allows dependencies to be optional if configured as such. When a dependency is optional, and no suitable candidate
is available, Dirk will either inject `null` (for constructor and method parameters) or skip injection completely (for 
field injection). For example:

    class Greeter {
        @Inject @Opt private String greeting = "Hello World";
    }

When no `String` candidate is available, this version of the `Greeter` class will fall back to a default greeting.
Using constructor injection the same can be achieved as follows:

    class Greeter {
        final String greeting;

        @Inject
        Greeter(@Opt String greeting) {
            this.greeting = greeting == null ? "Hello World" : greeting;    
        }
    }

This allows a convenient way to have default values for, for example, configuration settings:

    class TimeProvider {
        @Inject @Opt @Named("server.ntp.url") String url = "pool.ntp.org";
        @Inject @Opt @Named("server.sync.interval") int interval = 3600;  // every hour
    }

## Customizing Injectors

The injector can be customized in several ways:

- The `InjectorStrategy` allows for detailed customization of how the injector works:
  - `AnnotationStrategy` is responsible for detecting injectable members and their qualifiers
  - `ScopeStrategy` is responsible for detecting scope annotations, and configuring default scope annotations
  - `ProxyStrategy` is responsible for creating proxies
  - `LifeCycleCallbacksFactory` controls which and in what order lifecycle methods are to be called
- Using `InjectionTargetExtension`s
  - Allows customizing injection targets of a specific generic interface type
    - Inject lists of instances using the `ListInjectionTargetExtension`
    - Inject sets of instances using the `SetInjectionTargetExtension`
    - Inject providers to allow runtime creation of instances using the `ProviderInjectionTargetExtension`
    - Inject CDI `Instance` types using the `InstanceInjectionTargetExtension`
- Using `TypeRegistrationExtension`s
  - Allows detecting additional candidates when a type is registered
    - Add additional candidates for each `@Produces` annotation using the `ProducesTypeRegistrationExtension`
    - Add a candidate when types implement the `Provider` interface
    - Assisted injection: When registering a SAM type (a class or interface with a single abstract method) automatically generate
      an implementation of this method which produces a new candidate, see `AssistedTypeRegistrationExtension`
- Using `ScopeResolver`s
  - Allows creating resolvers for custom scope annotations 
- The injector can be configured to only provide candidates explicitly registered with it or also allow auto 
  discovery of additional candidates through their dependencies

## Extensions

### Type Registration Extensions

A `TypeRegistrationExtension` is called whenever a new type is encountered by the injector during registration.
The extension is given the opportunity to register additional types that it can derive from the new type. Usually,
additional types are derived by inspecting the given type for annotations or interfaces it implements.

### Injection Target Extension

An `InjectionTargetExtension` allows creating instances of a specific generic interface prior to injection.
The interface must have at least one type variable which the extension must expose to the injector as its element 
type. The element type, together with any qualifiers on the injection target, determines which candidates can be 
used by the extension to provide the interface implementation. The extension is allowed to delay the lookup or
perform it immediately. 

Interfaces which are extended by an injection target extension can no longer be directly injected, even if a
candidate implements the interface.

Examples are the `ListInjectionTargetExtension` and the `ProviderInjectionTargetExtension`, which provide support for
injecting lists of candidates or delayed lookup of candidates (through the `List` and `Provider` interfaces)
respectively.

Extensions which do delayed lookup are allowed to wrap any other extended type, but in all other cases extensions 
cannot be nested.

# The Core Library

Dirk's library and extension modules provide several standard extensions, strategies and scope resolvers that are 
normally integrated directly in most dependency injection frameworks. These are maintained as part of the project
and can be used as desired, or serve as a starting point for 3rd party customizations.

## Injection Target Extensions

- `ListInjectionTargetExtension` supports injecting multiple matching candidates as instances in a `List`.
- `SetInjectionTargetExtension` supports injecting multiple matching candidates as instances in a `Set`.
- `ProviderInjectionTargetExtension` supports indirect access to candidate instances using a
  configurable interface (often the `Provider` interface)
  
## Type Registration Extensions

### `ProducesTypeRegistrationExtension`

Allows fields and methods to act as factories for additional candidates using a configurable producer annotation.
Producer methods with parameters are considered to offer candidates with dependencies which are to be provided when the
method is called.

The following examples assume the annotation `@Produces` is configured to mark producer fields and methods:

A field which produces a `String`:

    @Produces String greeting = "Hello World";

A method which produces a `Connection` given a URI:

    @Produces Connection createConnection(URI uri) { ... }

Producers can be static or non-static. Static producers can be called at any time and have no access to any
dependencies injected in their owner instance. Non-static producers can either have dependencies provided as parameters
(in the case of a producer method) or by accessing fields in their owner instance.

### `ProviderTypeRegistrationExtension`

Allows a type to act as a factory for an additional candidate when it implements a configurable interface (typically, 
the `Provider` interface).

Below an example of a class that implements the `Provider` interface and provides an additional candidate `Connection`:

    class ConnectionProvider implements Provider<Connection> {
        @Inject URI uri;

        @Override
        public Connection get() { ... }
    }

### `AssistedTypeRegistrationExtension`

Allows a type which has a single abstract method (a SAM type) with a non-void return type to act as a factory for the 
returned type; any dependencies the produced type may have are injected by the injector, including the arguments 
supplied to the factory method; this is otherwise known as assisted injection.

Assume we have a candidate that requires an additional parameter at runtime before it can be constructed, but also
has a required dependency:

    class Greeter {
        @Inject String greeting;  // a normal dependency
        @Inject @Argument LocalTime timeOfDay;

        void greet() { ... }
    }

With this extension we can have Dirk generate a factory that can be injected, and which takes the additional argument
as a parameter:

    @Inject Function<LocalTime, Greeter> greeterFactory;

The factory can be called as a normal `Function`:

    Greeter greeter = greeterFactory.apply(LocalTime.now());
 
    greeter.greet();

Any SAM type can be used, as long as the arguments its method accepts can be matched up with the arguments in the 
produced type. The following two examples would also allow constructing the `Greeter` type:

    interface GreeterFactory {
        Greeter createGreeter(LocalTime timeOfDay);
    }

... and:

    abstract class GreeterFactory {
        Greeter createGreeter(LocalTime timeOfDay);
    }

Important note: in order for Dirk to match up the argument names, classes should be compiled with parameter name
information (use the `-parameters` flag for `javac`). Alternatively, the names can be explicitly specified with the 
`@Argument` annotation:

    abstract class GreeterFactory {
        Greeter createGreeter(@Argument("timeOfDay") LocalTime timeOfDay);
    }

# BSD License

Copyright (c) 2013-2022, John Hendrikx
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Dependencies and Acknowledgments

### Reflections

License: WTFPL (http://www.wtfpl.net/)  
https://github.com/ronmamo/reflections

Linking: Maven Dependency

Used for:
- Scanning classpath for annotations

### Geantyref

License: Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0)   
https://github.com/leangen/geantyref

Linking: Maven Dependency

Used for:
- Implementation of Annotation interface

### Apache Commons Lang

License: Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0)  
https://commons.apache.org/proper/commons-lang/   

Linking: Embedded (minimal required code directly included)

Used for:
- `TypeUtils` for dealing with generic types and type variables

### Byte Buddy

License: Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0)  
https://bytebuddy.net/

Linking: Maven Dependency (optional)

Used for:
- Generating factories for Assisted Injection
- Generating proxies
