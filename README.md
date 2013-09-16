# Reactor Data

This is an early draft of enabling Reactor's Composable to provide asynchronous data access even if the underlying datastore access is fully-blocking.

[![Build Status](https://drone.io/github.com/reactor/reactor-data/status.png)](https://drone.io/github.com/reactor/reactor-data/latest)

### Spring Data Repositories

This submodule provides an interface similar to Spring Data's [CrudRepository](http://docs.spring.io/spring-data/data-commons/docs/1.6.1.RELEASE/api/org/springframework/data/repository/CrudRepository.html) but replaces references to domain objects with references to `Composable<DomainObject>`. This gives you a chainable, composable data API that doesn't block the caller while waiting on data (it uses a background IO thread instead).

Given a domain object called `Person`, and a Spring Data Repository to manage it, you just have to declare a subclass of `ComposableCrudRepository` and you will get a fluent composable API that lets you do map, filter, and other composable actions like the following silly example (using JDK 8 lambdas):

    @Inject
    ComposablePersonRepository people;

    Promise<String> fullName = people.findOne(personId).map(p -> p.getFirstName() + " " + p.getLastName());

You can block if you want by calling Promise's `await(long timeout, TimeUnit unit)` method.

    String s = fullName.await(5, TimeUnit.SECONDS); // block caller for 5 seconds

Or you can do a non-blocking call by composing actions.

    fullName.map(s -> /* do something with String fullName */)


