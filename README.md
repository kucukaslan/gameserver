# dream

## Build and Run
First the database must be created.
I've exported the database (structure) to [./extras/dream.sql](./extras/dream.sql) file.
Create a database and use that sql file to create the tables.

in [./config.properties](./config.properties) file, set the database credentials.
there are my dev. environment credentials, I will commit them for convenience.

for the moment I'm not using redis, so you can ignore the redis properties. (I might use it later)

```bash
# mvn clean package
mvn spring-boot:run
```
Test:
```bash
mvn test
```


## Explanations & Discussions
### A few Notes:
- I've used MySQL as a database (to be pedantic I used the MariaDB that comes with XAMPP)
- Database configuration is in [./config.properties](./config.properties) file, 
I will commit it for convenience with the test db credentials.
- I've exported the database (structure) to [./extras/dream.sql](./extras/dream.sql) file. 
- I've also exported the database (structure and data) to [./extras/dream_with_data.sql](./extras/dream_with_data.sql) file.
however some of those data are manually modified, so they might be in 
invalid (unattainable) state. e.g. some of the users referenced by tournament groups
are deleted etc. 
- The program has an extensive logging frequency. You can alter the log level, by changing the relevant values [src/main/resources/application.properties](src/main/resources/application.properties)
My suggestion is not to set it to DEBUG or TRACE if the performance is important.
Though, I prefer setting this project to TRACE, and rest (`root`) to INFO during development.

### The Program
> [!NOTE]  
> there are some comments in the code and the [Journal.md](Journal.md) file that might be relavant

#### General
- The program is a Spring Boot application. 
- There are UserController and TournamentController classes that handle the requests. 
- There is also a TournamentManager class that is responsible for managing the tournaments. Request handlers of TournamentController usually delegate the request to TournamentManager.
- There is a MyUtil class that contains some utility methods such as setting/retrieving `traceId`s for the requests for the logging purposes.
- There is DBService class that is responsible for database operations. I used JDBC with hand-written SQL queries. I thought of using JPA, but I don't have much experience with it, especially I'm not sure how can (or can) I use it to write relatively complex queries that require multiple joins etc. I was also short on time to learn it, so let it go after some time. \
It might be irrelevant, especially for this project, but I'm sceptical about the performance of JPA especially for the bulk operations. Though, I don't want to make any claims without actually testing and/or understanding it.
- Generally I've used JSONObjects for flexibility, especially the use case is
limited to get/set operations. But it might have been to use POJOs for some use cases. 

#### Time and Timezones
- I've consistently used UTC time for every time related operation.
- I've used `UTC_TIMESTAMP` in SQL, I connected to DB with `connectionTimeZone=UTC`,
I called `Calendar.getInstance(TimeZone.getTimeZone("UTC"))` in Java code.
HOWEVER, you know timezones are [timezones](https://www.zainrizvi.io/blog/falsehoods-programmers-believe-about-time-zones/) 
there is always, literally always, some mistakes. I suspect that during summer time zone
changes the program might incorrectly clear the tournament group queues depending
 on the server's local time etc. but forgive me for omitting that :)

- I do evaluate the tournament requests based on the server's time.
  It's bad (luck?) if a user sends a request to join tournament at 19.59.59.999 and the server
    receives it at 20.00.00.001 and rejects it. But I guess the user probably won't
    score much in that 1 second anyway :)
A potential problem might be in claimrewards endpoint though. If a user's updatelevel
request is received at 19.59.59.999 and the server normally processes it. But before
the server processes the claimreward request, another user from same group may send
a request to claimreward. In that case the reward might not be given accurately.
We could add a buffer time for all procceesing to complete before announcing the results. But I am not sure what is the best way to handle this. We may at someplace
use counters (REDIS counters `INCR` if there are multiple servers) to keep track of
the number of requests being processed so that claimreward endpoint can wait for
it to be zeroeth. But forgive me for not implementing it too :)


#### Endpoints
There are 7 endpoints. Besides the "Enter Tournament" endpoint all of them are straightforward implementations of the requirements (Each request is processed independently).
```
1. check request if it contains valid data in valid format
2. check if the prerequisite conditions are met (e.g. does the user have enough coins to join a tournament?)
3. if the request is valid, then process it
4. return the result (both success and failure)
```

The "Enter Tournament" endpoint is a bit more complicated.
In addition to usual checks we need to wait for other players to join the tournament.
So it depends on other enter tournament requests. The callee will wait until the tournament group is formed. That is until other players join the tournament.

To explain it further, the program wants to make online/real-time player matching.
So it waits for sufficient number of players to request to join the tournament. 
Then match them and start tournament for their group.

It was requested that each tournament group should have 1 player from each country.
The intuitive way to do this is to have queues for each country that holds Players
and when an empty queue is filled with a player (check if all queues are non-empty)
then start the tournament by dequeueing the players from the queues. 
This is a valid and reasoaable approach to do so. However, given that I need to 
include group details and ranking in the request, I made a small change to simplify
communication. Instead of adding players to queues, I added the group (candidates) to the queue.
It may sound a bit counter-intuitive and weird at first, but it is actually a reasonable
approach (at least reasonable to some people -me). 

I hope the following description will make it clear.

At the very first request to enter tournament coming from user of country `X`,
the program will create a TournamentGroup (JSON)object and put that object to the 
queues of **other** countries. So when a user from country `Y` requests to enter tournament,
it will find the group object in the queue of `Y` and add itself to the group.
If for a user from a country (let say `X`) there is no group object in the queue of `X`,
then it will create a new group object and put it to the queues of other countries.


So the queues actually hold how many users from other countries is needed to form a group.
Unlike the other approach that holds the users from that country waiting to play. 
To give an example, in my approach if there are 2 objects in the queue of `X` then it means
that there are 2 groups waiting for a player from `X` to join. In the other approach it would mean
that there are 2 players from `X` waiting to play. The difference is subtle but important.

when a user dequeues a group object and detects that it is full then a tournamentgroup is formed
for that group, the (threads of) other users of that group are notified so that they can both 
insert themselves to the DB and return the response to the requests. (I have a separate DB table that stores the tournament group and user relation along with metadata such as score and whether user has claimed the award etc. Given that group size is fixed to 5 we could've added 10 or so columns to the tournament group table, but you may never know how fixed `fixed size` is :) )

I've used `ConcurrentLinkedQueue`s. I also used syncronized block/methods/objects when needed. Obviously for, aforementioned, notification of other threads
I used the Java's built-in `wait()` and `notify()` methods. 

#### Tests
I've written some test especially for the failure cases. However, for some reason,
I couldn't use annotations like @BeforeTestMethod etc. It made writing tests 
much more difficult and irritating. Especially for this server we have various
steps to be taken inorderly: e.g. user created, user reaches level 20, user joins tournament,
tournament ends, user checks rank, user claims rewards, user joins another tournament etc.

I guess my mistake is possibly related to some behaviour of Spring Boot which is usually
configured by default/automatically but for this particular case I probably made a mistake 
that I would need my seniors help to fix.

So the tests does not cover all the edgecases that it could, should, have.

## Possible TODOs
- [ ] using local cache instead of redis? (for now I'm not using redis)
TournamentManager has a built-in cache, but it is not persistent. Maybe I can use redis for that.
```java
    private Map<String, JSONObject> tournamentCacheByCode;
```
- [x] make sure the tournament joint request queues are emptied after the tournament ends.
so that the next tournament can start with empty queues.
- [x] User can join tournaments only if it has enough coins to join.
- [x] The user can earn coins only for completing level or winning (ranked 1st or 2nd) tournament.
- [x] should add checks for whether user is joined to tournament
- [x] implement claim award
- [x] fix the sleep thing
- [ ] add tests 
- [x] check: server will convert its time to UTC and process tournament events accordingly

## Questions
- [ ] endpoint access control (should require tokens?)

## Journal
There is also [Journal.md](Journal.md) file that I kept while working on this project. 
It is like a diary or an informal `git history`.