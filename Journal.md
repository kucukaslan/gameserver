# Journal
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

