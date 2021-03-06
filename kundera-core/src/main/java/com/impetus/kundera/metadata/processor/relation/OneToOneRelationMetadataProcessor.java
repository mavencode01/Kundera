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
package com.impetus.kundera.metadata.processor.relation;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import com.impetus.kundera.loader.MetamodelLoaderException;
import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.Relation;
import com.impetus.kundera.metadata.model.attributes.AbstractAttribute;
import com.impetus.kundera.metadata.processor.AbstractEntityFieldProcessor;
import com.impetus.kundera.metadata.validator.EntityValidatorImpl;

/**
 * The Class OneToOneRelationMetadataProcessor.
 * 
 * @author Amresh Singh
 */
public class OneToOneRelationMetadataProcessor extends AbstractEntityFieldProcessor implements
        RelationMetadataProcessor
{

    /**
     * Instantiates a new one to one relation metadata processor.
     */
    public OneToOneRelationMetadataProcessor()
    {
        validator = new EntityValidatorImpl();
    }

    @Override
    public void addRelationIntoMetadata(Field relationField, EntityMetadata metadata)
    {
        // taking field's type as foreign entity, ignoring "targetEntity"
        Class<?> targetEntity = relationField.getType();
        validate(targetEntity);

        // TODO: Add code to check whether this entity has already been
        // validated, at all places below

        OneToOne oneToOneAnn = relationField.getAnnotation(OneToOne.class);

        boolean isJoinedByPK = relationField.isAnnotationPresent(PrimaryKeyJoinColumn.class);
        boolean isJoinedByFK = relationField.isAnnotationPresent(JoinColumn.class);
        boolean isJoinedByTable = relationField.isAnnotationPresent(JoinTable.class);

        /*
         * if(!isJoinedByPK && !isJoinedByFK) { throw new PersistenceException(
         * "A one-to-one relationship must have either JoinColumn or PrimaryKeyJoinColumn annotation"
         * ); }
         */

        Relation relation = new Relation(relationField, targetEntity, null, oneToOneAnn.fetch(),
                Arrays.asList(oneToOneAnn.cascade()), oneToOneAnn.optional(), oneToOneAnn.mappedBy(),
                Relation.ForeignKey.ONE_TO_ONE);

        if (isJoinedByPK)
        {
            
            relation.setJoinedByPrimaryKey(true);
            EntityMetadata joinClassMetadata = KunderaMetadataManager.getEntityMetadata(targetEntity.getClass());
            relation.setJoinColumnName(joinClassMetadata != null? ((AbstractAttribute)joinClassMetadata.getIdAttribute()).getJPAColumnName(): null);
        }
        else if (isJoinedByFK)
        {
            JoinColumn joinColumnAnn = relationField.getAnnotation(JoinColumn.class);
            relation.setJoinColumnName(joinColumnAnn.name());
        }
        else if (isJoinedByTable)
        {
            throw new UnsupportedOperationException("@JoinTable not supported for one to one association");
/*            JoinTableMetadata jtMetadata = new JoinTableMetadata(relationField);
            relation.setRelatedViaJoinTable(true);
            relation.setJoinTableMetadata(jtMetadata);
*/        }

        relation.setBiDirectionalField(metadata.getEntityClazz());
        metadata.addRelation(relationField.getName(), relation);
        
    }

    @Override
    public void process(Class<?> clazz, EntityMetadata metadata)
    {
        throw new MetamodelLoaderException("Method call not applicable for Relation processors");
    }

}
