# Journal
Time I spent on this project might seem too much, but I've intertwined it with
experimenting with Spring and Spring Data JPA and adding some functionalities 
that are not normally required for this project. I used the project as an excuse
to learn more about some topics (e.g. logging, redis streams, redis queues (lists) )
which may not even be used in the project.


## 11.10
2 and half hours
- Create MySQL DB and draft tables schematic:
![schematic](extras/dream.svg)
- Create a new Spring project with barebones (add endpoints, that's it)

## 11.11-12
1 hour
- I had established basic DB connection with handwriten SQL queries but that
level of rawness seems to be painful to maintain. Not that I can't write SQL statements,
really that is easy part, but I don't want to write all the boilerplate code
error handling, table/column names, etc.  
For the moment the kind of queries I need are simple enough to be handled by
Spring Data JPA. Especially, since, each request is a single query, so I don't get to
entertain the idea of using a single transaction for multiple queries. Even if I
write raw SQL queries.

## 11.13
2 and half hours
- I've implemented CreateUserRequest updateLevelRequest endpoints. though the latter
will need to be updated to handle tournament scorekeeping if there any.
