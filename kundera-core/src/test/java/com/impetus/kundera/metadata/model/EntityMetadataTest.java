/*******************************************************************************
 * * Copyright 2012 Impetus Infotech.
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
package com.impetus.kundera.metadata.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.impetus.kundera.Constants;
import com.impetus.kundera.PersistenceProperties;
import com.impetus.kundera.client.CoreTestClientFactory;
import com.impetus.kundera.configure.ClientFactoryConfiguraton;
import com.impetus.kundera.configure.SchemaConfiguration;
import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.processor.IndexProcessor;
import com.impetus.kundera.metadata.processor.TableProcessor;
import com.impetus.kundera.persistence.EntityManagerFactoryImpl;
import com.impetus.kundera.persistence.event.PersonEventDispatch;

/**
 * @author Kuldeep Mishra
 * 
 */
public class EntityMetadataTest
{
    private String persistenceUnit = "metaDataTest";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        getEntityManagerFactory(null);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        KunderaMetadata.INSTANCE.setApplicationMetadata(null);
    }

    @Test
    public void test()
    {
        EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(Employe.class);
        Assert.assertNotNull(entityMetadata);
        Assert.assertNotNull(entityMetadata.getIndexProperties());
        Assert.assertFalse(entityMetadata.getIndexProperties().isEmpty());
        Assert.assertEquals(2, entityMetadata.getIndexProperties().size());
        Assert.assertNotNull(entityMetadata.getIndexProperties().get("AGE"));
        Assert.assertNotNull(entityMetadata.getIndexProperties().get("EMP_NAME"));
        Assert.assertNull(entityMetadata.getIndexProperties().get("departmentData"));
        Assert.assertNotNull(entityMetadata.toString());

        Map<String, PropertyIndex> indexes = IndexProcessor.getIndexesOnEmbeddable(Department.class);
        Assert.assertNotNull(indexes);
        Assert.assertFalse(indexes.isEmpty());
        Assert.assertEquals(2, indexes.size());

        Assert.assertNotNull(indexes.get("email"));
        Assert.assertEquals("ASC", indexes.get("email").getIndexType());
        Assert.assertEquals(new Integer(Integer.MAX_VALUE), indexes.get("email").getMax());
        Assert.assertEquals(new Integer(Integer.MIN_VALUE), indexes.get("email").getMin());

        Assert.assertNotNull(indexes.get("location"));
        Assert.assertEquals("GEO2D", indexes.get("location").getIndexType());
        Assert.assertEquals(new Integer(200), indexes.get("location").getMax());
        Assert.assertEquals(new Integer(-200), indexes.get("location").getMin());     
        
    }

    @Test
    public void testEmbeddedCollection()
    {
        EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(KunderaUser.class);
        Assert.assertNotNull(entityMetadata);
        Assert.assertTrue(entityMetadata.getIndexProperties().isEmpty());
        Assert.assertEquals(EntityMetadata.Type.SUPER_COLUMN_FAMILY, entityMetadata.getType());
        Assert.assertNotNull(entityMetadata.toString());
        
        entityMetadata.setCounterColumnType(false);
        Assert.assertFalse(entityMetadata.isCounterColumnType());       
    }

    /**
     * Gets the entity manager factory.
     * 
     * @param useLucene
     * @param property
     * 
     * @return the entity manager factory
     */
    private EntityManagerFactoryImpl getEntityManagerFactory(String property)
    {
        ClientMetadata clientMetadata = new ClientMetadata();
        Map<String, Object> props = new HashMap<String, Object>();

        props.put(Constants.PERSISTENCE_UNIT_NAME, persistenceUnit);
        props.put(PersistenceProperties.KUNDERA_CLIENT_FACTORY, CoreTestClientFactory.class.getName());
        props.put(PersistenceProperties.KUNDERA_NODES, "localhost");
        props.put(PersistenceProperties.KUNDERA_PORT, "9160");
        props.put(PersistenceProperties.KUNDERA_KEYSPACE, "KunderaMetaDataTest");
//        props.put(PersistenceProperties.KUNDERA_INDEX_HOME_DIR, "lucene");
        clientMetadata.setLuceneIndexDir(null);

        KunderaMetadata.INSTANCE.setApplicationMetadata(null);
        ApplicationMetadata appMetadata = KunderaMetadata.INSTANCE.getApplicationMetadata();
        PersistenceUnitMetadata puMetadata = new PersistenceUnitMetadata();
        puMetadata.setPersistenceUnitName(persistenceUnit);
        Properties p = new Properties();
        p.putAll(props);
        puMetadata.setProperties(p);
        Map<String, PersistenceUnitMetadata> metadata = new HashMap<String, PersistenceUnitMetadata>();
        metadata.put(persistenceUnit, puMetadata);
        appMetadata.addPersistenceUnitMetadata(metadata);

        Map<String, List<String>> clazzToPu = new HashMap<String, List<String>>();

        List<String> pus = new ArrayList<String>();
        pus.add(persistenceUnit);
        clazzToPu.put(Employe.class.getName(), pus);
        clazzToPu.put(KunderaUser.class.getName(), pus);

        appMetadata.setClazzToPuMap(clazzToPu);

        EntityMetadata m = new EntityMetadata(Employe.class);
        EntityMetadata m1 = new EntityMetadata(KunderaUser.class);       

        TableProcessor processor = new TableProcessor(null);
        processor.process(Employe.class, m);
        processor.process(KunderaUser.class, m1);

        IndexProcessor indexProcessor = new IndexProcessor();
        indexProcessor.process(Employe.class, m);
        indexProcessor.process(KunderaUser.class, m1);        
        Assert.assertNotNull(m1.toString());

        m.setPersistenceUnit(persistenceUnit);

        MetamodelImpl metaModel = new MetamodelImpl();
        metaModel.addEntityMetadata(Employe.class, m);
        metaModel.addEntityMetadata(KunderaUser.class, m1);

        appMetadata.getMetamodelMap().put(persistenceUnit, metaModel);

        metaModel.assignManagedTypes(appMetadata.getMetaModelBuilder(persistenceUnit).getManagedTypes());
        metaModel.assignEmbeddables(appMetadata.getMetaModelBuilder(persistenceUnit).getEmbeddables());
        metaModel.assignMappedSuperClass(appMetadata.getMetaModelBuilder(persistenceUnit).getMappedSuperClassTypes());

//        KunderaMetadata.INSTANCE.addClientMetadata(persistenceUnit, clientMetadata);

        String[] persistenceUnits = new String[] { persistenceUnit };
        new ClientFactoryConfiguraton(null, persistenceUnits).configure();

        new SchemaConfiguration(null, persistenceUnits).configure();
        // EntityManagerFactoryImpl impl = new
        // EntityManagerFactoryImpl(puMetadata, props);
        return null;
    }
    
    @Test
    public void testCallbackMethods()
    {
        Map<String, Object> props = new HashMap<String, Object>();
      props.put("index.home.dir","lucene");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kunderatest", props);
        EntityManager em = emf.createEntityManager();   
        
        EntityMetadata m = KunderaMetadataManager.getEntityMetadata(PersonEventDispatch.class);
        Assert.assertNotNull(m.toString());
        
        PersonEventDispatch person = new PersonEventDispatch();
        person.setFirstName("vivek");
        person.setLastName("mishra");
        person.setPersonId("1_p");
        
        em.persist(person);
        
        em.clear();
        
        PersonEventDispatch result = em.find(PersonEventDispatch.class, "1_p");
        Assert.assertEquals(result.getLastName(), "Post Load");
        
        onFindCallBack(em);
        em.refresh(result);
        
        Assert.assertEquals(result.getLastName(), "Post Load");
        
        
        em.close();
        emf.close();
    }

    
    private void onFindCallBack(EntityManager em)
    {
        String query = "Selcet p from PersonEventDispatch p";
        
        Query q = em.createQuery(query);
        
        List<PersonEventDispatch> results = q.getResultList();
        
        Assert.assertNotNull(results);
        Assert.assertFalse(results.isEmpty());
        Assert.assertEquals(1, results.size());
    }
    
    @Test
    public void testColumn()
    {
        try
        {
            Column column = new Column("EMP_NAME", Employe.class.getDeclaredField("empName"));
            column.setIndexable(true);
            Assert.assertTrue(column.isIndexable());
            Assert.assertEquals("empName", column.getField().getName());
            Assert.assertEquals("EMP_NAME", column.getName());
            
            column = new Column("AGE", Employe.class.getDeclaredField("age"), true);            
            Assert.assertTrue(column.isIndexable());
            Assert.assertEquals("age", column.getField().getName());
            Assert.assertEquals("AGE", column.getName());
        }
        catch (SecurityException e)
        {            
            Assert.fail(e.getMessage());
        }
        catch (NoSuchFieldException e)
        {            
            Assert.fail(e.getMessage());
        }
    }
}
