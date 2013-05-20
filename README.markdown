Dynamic Dependency Injection Framework
======================================

This is a light-weight framework that allows you to use standard JSR-330 javax.inject 
Annotations to create instances of objects, even when they're dynamically loaded at 
runtime.  This framework will allow you to package your classes in seperate JAR's,
load them at runtime, and have them injected with dependencies or serve as dependencies
for other classes.

For example given a class implementing the `PaymentProvider` interface:

    public class CreditCardPaymentProvider implements PaymentProvider {
    }

The below class would get its `paymentProvider` field injected with an instance of
the above class:

    public class BookShop {
        @Inject
        private PaymentProvider paymentProvider;
    }

This framework differs from most standard DI frameworks that dependencies can be
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
* Can inject classes loaded at runtime
* Can use classes loaded at runtime as injection candidates
* Supports JSR-330
* Allows multiple Qualifiers for injectables as well as injections

Requirements
------------
* Java Runtime Environment 1.7+ installed

Getting started
---------------
Todo...

Open issues
-----------
* No optional dependencies yet (JSR-330 does not support them, so it still has to be decided how those can be added without becoming incompatible)
* Does not yet support the JSR-330 Scope annotation (although Singleton is supported)
* Many Qualifiers on a single class (10+) will probably cause issues, somekind of limit needs to be enforced to prevent this or the issue needs to be addressed with a better solution
* No method injection support yet
* Exception messages probably need to be improved to be more clear
* Injector is not thread-safe; it is still undecided if it should be thread-safe (which can affect its speed) or that external synchronisation is the solution
* Circular dependencies are not supported (since the Injector checks dependencies when registering a class, it will never allow anything to be registered that has missing dependencies).  It is unclear if support for these is desired at all.


Dependencies and Acknowledgements
=================================

Reflections
-----------
License: WTFPL (http://www.wtfpl.net/)  
https://code.google.com/p/reflections/

DirectedGraph and TopologicalSort
---------------------------------
by Keith Schwarz  
License: Public Domain  
http://www.keithschwarz.com/interesting/