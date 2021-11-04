Dynamic Dependency Injection Framework
======================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.hjohn.ddif/ddif-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hjohn.ddif/ddif-core)
[![Build Status](https://travis-ci.org/hjohn/hs.ddif.svg?branch=master)](https://travis-ci.org/hjohn/hs.ddif)
[![Coverage Status](https://coveralls.io/repos/github/hjohn/hs.ddif/badge.svg?branch=master)](https://coveralls.io/github/hjohn/hs.ddif?branch=master)

This is a light-weight framework that allows you to use standard JSR-330 javax.inject
Annotations to create instances of objects, even when they're dynamically loaded at
runtime.  This framework will allow you to package your classes in seperate JAR's,
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

You may ask, if dependencies are enforced in such a way, how would it ever be possible
to depend on something that a dynamically loaded class offers?  This can be achieved by
using optional dependencies (not supported by JSR-330, and currently not yet supported
by this framework) and by using Collection dependencies.

An example of a Collection dependency:

    public class BookShop {
        @Inject
        private Set<Book> booksOnShelf;
    }

Now, if there are no books registered with the Injector, it will simply inject an
empty collection.  However if at creation time of the BookShop instance there are
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

Features
--------
* Injects classes with dependencies, even if loaded at runtime
* Injects collections
* Injection candidates can be supplied directly and/or loaded at runtime
* Supports JSR-330
* Just-in-time discovery of dependencies
* Jars can be scanned for injection candidates to be included in an injector
* Allows multiple Qualifiers for injection candidates as well as injections
* Supports injection of generic types

Requirements
------------
* Java Runtime Environment 1.7+ installed

Getting started
---------------
Todo...

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

Now we can ask the Injector to create us an instance of ``BookShop``:

    BookShop bookShop = injector.getInstance(BookShop.class);

This will create a ``BookShop`` instance by calling the constructor annotated with ``@Inject`` and providing it with
an instance of ``CreditCardPaymentProcessor``.  The ``booksOnShelf`` field will be injected (after the Constructor
has completed) with an empty Set as no ``Book`` injection candidates are currently known to the Injector.

By registering some ``Book`` objects, and asking for a new Instance of ``BookShop`` we can get the ``booksOnShelf`` field
populated with some books:

    injector.registerInstance(new Book("Dune"));
    injector.register(SomeBook.class);

    BookShop anotherBookShop = injector.getInstance(BookShop.class);

The second ``BookShop`` instance will have a ``Set`` of ``Book`` objects matching those known by the Injector.  If you
wanted to have all ``BookShop`` instances to have access to the latest known books, you can wrap the collection
with a ``Provider``.  The ``BookShop`` instances can then query for the latest books by calling the ``get`` method of
the ``Provider`` any time they want:

    public class BookShop {

        @Inject
        private Provider<Set<Book>> booksOnShelf;

        public Set<Book> getLatestBooks() {
            return booksOnShelf.get();
        }

        ...
    }

### Just-in-time dependencies

Registering every dependency manually (and in the correct order) can quickly become tedious.  In our above
example, the ``BookShop`` class needed to be registered as well as its ``CreditCardPaymentProcessor`` dependency.
We can however have the Injector discover these automatically, as long as the dependencies themselves are
concrete classes that have a default constructor or exactly one constructor with the ``@Inject`` annotation.

In order to discover dependencies automatically, we have to create the Injector slightly differently:

    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

Now we can get an instance of ``BookShop`` without any further dependencies needing to be registered:

    BookShop bookShop = injector.getInstance(BookShop.class);

Under the hood, the Injector will notice there is no ``BookShop`` injection candidate registered.  However,
by analyzing the ``BookShop`` class it sees that it can be instantiated with a Constructor that requires a
``CreditCardPaymentProcessor`` -- unfortunately, there is also no ``CreditCardPaymentProcessor`` registered.  The
Injector then recursively analyzes the ``CreditCardPaymentProcessor`` class, and registers this class with
itself as it noticed that it can be simply instantiated with a default constructor.

Now that the dependencies for ``BookShop`` can also be satisfied, the ``BookShop`` class is registered with the Injector
and an instance is returned.  

Open issues
-----------
* No optional dependencies yet (JSR-330 does not support them, so it still has to be decided how those can be added without becoming incompatible)
* Does not yet support the JSR-330 Scope annotation (although Singleton is supported)
* Many Qualifiers on a single class (10+) will probably cause issues, some kind of limit needs to be enforced to prevent this or the issue needs to be addressed with a better solution
* No method injection support yet
* Exception messages probably need to be improved to be more clear
* Injector is not thread-safe; it is still undecided if it should be thread-safe (which can affect its speed) or that external synchronisation is the solution
* Circular dependencies are not supported (since the Injector checks dependencies when registering a class, it will never allow anything to be registered that has missing dependencies).  It is unclear if support for these is desired at all.
* Only java.util.Set and java.util.List are currently supported for collection injection (but easy to extend)
* Collection injection conflicts with injecting classes that extend a collection interface

BSD License
-----------
Copyright (c) 2013, John Hendrikx
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Dependencies and Acknowledgements
---------------------------------

### Reflections

License: WTFPL (http://www.wtfpl.net/)  
https://code.google.com/p/reflections/

### DirectedGraph and TopologicalSort

by Keith Schwarz
License: Public Domain
http://www.keithschwarz.com/interesting/