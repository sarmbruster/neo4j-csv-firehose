package org.neo4j.extension.firehose

import groovy.sql.Sql
import org.junit.rules.ExternalResource

/**
 * create a prepopulated in-memory H2 instance
 */
class SampleDbRule extends ExternalResource {

    Sql db
    String jdbc = "jdbc:h2:mem:devDb"

    @Override
    protected void before() throws Throwable {
        db = Sql.newInstance(jdbc)
        db.execute("""
create table person (
   id int auto_increment primary key,
   name varchar(20)

)""")
        db.execute("insert into person (name) values (?)", ["John"])
        assert db.updateCount == 1

        db.execute("insert into person (name) values (?)", ["Jim"])
        assert db.updateCount == 1

    }

    @Override
    protected void after() {
        db?.execute("drop table person")
        db?.close()
    }
}
