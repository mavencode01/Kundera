/*******************************************************************************
 * * Copyright 2013 Impetus Infotech.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 ******************************************************************************/
package com.impetus.client.es;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.impetus.client.es.PersonES.Day;
import com.impetus.kundera.PersistenceProperties;
import com.impetus.kundera.metadata.model.KunderaMetadata;

/**
 * @author vivek.mishra junit to demonstrate ESQuery implementation.
 */
public class PersonESTest
{

    /** The emf. */
    private EntityManagerFactory emf;

    /** The em. */
    private EntityManager em;

    @Before
    public void setup()
    {
        KunderaMetadata.INSTANCE.setApplicationMetadata(null);
        emf = Persistence.createEntityManagerFactory("es-pu");
        em = emf.createEntityManager();
    }

    @Test
    public void testWithBatch() throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException, InterruptedException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PersistenceProperties.KUNDERA_NODES, "localhost");
        props.put(PersistenceProperties.KUNDERA_PORT, "9300");
        props.put(PersistenceProperties.KUNDERA_BATCH_SIZE, 10);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("es-pu", props);
        EntityManager em = emf.createEntityManager();

        PersonES person = new PersonES();

        for (int i = 1; i <= 25; i++)
        {
            person.setAge(i);
            person.setDay(Day.FRIDAY);
            person.setPersonId(i + "");
            person.setPersonName("vivek" + i);
            em.persist(person);

            if (i % 10 == 0)
            {
                em.clear();
                for (int i1 = 1; i1 <= 10; i1++)
                {
                    PersonES p = em.find(PersonES.class, i1 + "");
                    Assert.assertNotNull(p);
                    Assert.assertEquals("vivek" + i1, p.getPersonName());
                }
            }

        }

        em.flush();
        em.clear();
        em.close();
        emf.close();

        // A scenario to mix and match with

        // 5 inserts, 5 updates and 5 deletes

        props.put(PersistenceProperties.KUNDERA_BATCH_SIZE, 50);
        emf = Persistence.createEntityManagerFactory("es-pu", props);
        em = emf.createEntityManager();

        for (int i = 21; i <= 25; i++)
        {
            person.setAge(i);
            person.setDay(Day.FRIDAY);
            person.setPersonId(i + "");
            person.setPersonName("vivek" + i);
            em.persist(person);
        }

        for (int i = 10; i <= 20; i++)
        {
            PersonES p = em.find(PersonES.class, i + "");

            if (i > 15)
            {
                em.remove(p);
            }
            else
            {
                p.setPersonName("updatedName" + i);
                em.merge(p);
            }

        }

        em.flush(); // explicit flush
        em.clear();

        // Assert after explicit flush

        for (int i = 10; i <= 15; i++)
        {
            if (i > 15)
            {
                Assert.assertNull(em.find(PersonES.class, i + "")); // assert on
                                                                    // removed
            }
            else
            {
                PersonES found = em.find(PersonES.class, i + "");
                Assert.assertNotNull(found);
                Assert.assertEquals("updatedName" + i, found.getPersonName());
            }
        }

        for (int i = 1; i <= 25; i++)
        {
            PersonES found = em.find(PersonES.class, i + "");
            if (found != null) // as some of record are already removed.
                em.remove(found);
        }

        // TODO: Update/delete by JPA query.
        // String deleteQuery = "Delete p from PersonES p";

        em.close();
        emf.close();
    }

    @Test
    public void testFindJPQL() throws InterruptedException
    {
        PersonES person = new PersonES();
        person.setAge(32);
        person.setDay(Day.FRIDAY);
        person.setPersonId("1");
        person.setPersonName("vivek");
        em.persist(person);

        person = new PersonES();
        person.setAge(32);
        person.setDay(Day.FRIDAY);
        person.setPersonId("2");
        person.setPersonName("kuldeep");
        em.persist(person);

        waitThread();
        String queryWithOutAndClause = "Select p from PersonES p where p.personName = 'vivek'";
        Query nameQuery = em.createNamedQuery(queryWithOutAndClause);

        List<PersonES> persons = nameQuery.getResultList();

        Assert.assertFalse(persons.isEmpty());
        Assert.assertEquals(1, persons.size());
        Assert.assertEquals("vivek", persons.get(0).getPersonName());

        String queryWithOutClause = "Select p from PersonES p";
        nameQuery = em.createNamedQuery(queryWithOutClause);

        persons = nameQuery.getResultList();

        Assert.assertFalse(persons.isEmpty());
        Assert.assertEquals(2, persons.size());

        String invalidQueryWithAndClause = "Select p from PersonES p where p.personName = 'vivek' AND p.age = 34";
        nameQuery = em.createNamedQuery(invalidQueryWithAndClause);
        persons = nameQuery.getResultList();

        Assert.assertTrue(persons.isEmpty());

        String queryWithAndClause = "Select p from PersonES p where p.personName = 'vivek' AND p.age = 32";
        nameQuery = em.createNamedQuery(queryWithAndClause);
        persons = nameQuery.getResultList();

        Assert.assertFalse(persons.isEmpty());
        Assert.assertFalse(persons.isEmpty());
        Assert.assertEquals(1, persons.size());
        Assert.assertEquals("vivek", persons.get(0).getPersonName());

        String queryWithORClause = "Select p from PersonES p where p.personName = 'vivek' OR p.personName = 'kuldeep'";
        nameQuery = em.createNamedQuery(queryWithORClause);
        persons = nameQuery.getResultList();

        Assert.assertFalse(persons.isEmpty());
        Assert.assertEquals(2, persons.size());

        String invalidQueryWithORClause = "Select p from PersonES p where p.personName = 'vivek' OR p.personName = 'lkl'";
        nameQuery = em.createNamedQuery(invalidQueryWithORClause);
        persons = nameQuery.getResultList();

        Assert.assertFalse(persons.isEmpty());
        Assert.assertEquals(1, persons.size());

        em.remove(em.find(PersonES.class, "1"));
        em.remove(em.find(PersonES.class, "2"));
        waitThread();
        // TODO: >,<,>=,<=
    }

    @After
    public void tearDown()
    {
        em.close();
        emf.close();
    }

    private void waitThread() throws InterruptedException
    {
        Thread.sleep(2000);
    }
}