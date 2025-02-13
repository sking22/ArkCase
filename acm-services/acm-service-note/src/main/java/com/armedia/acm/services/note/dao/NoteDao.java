package com.armedia.acm.services.note.dao;

/*-
 * #%L
 * ACM Service: Note
 * %%
 * Copyright (C) 2014 - 2018 ArkCase LLC
 * %%
 * This file is part of the ArkCase software. 
 * 
 * If the software was purchased under a paid ArkCase license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * ArkCase is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * ArkCase is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ArkCase. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.armedia.acm.data.AcmAbstractDao;
import com.armedia.acm.services.note.model.Note;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

public class NoteDao extends AcmAbstractDao<Note>
{
    private final Logger LOG = LogManager.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected Class<Note> getPersistenceClass()
    {
        return Note.class;
    }

    public List<Note> listNotes(String type, Long parentId, String parentType)
    {
        // Preconditions.checkNotNull(type, "Note type cannot be null");
        Preconditions.checkNotNull(parentId, "Parent Id cannot be null");
        Preconditions.checkNotNull(parentType, "Parent type cannot be null");

        TypedQuery<Note> note = getEntityManager().createQuery(
                "SELECT note " +
                        "FROM Note note " +
                        "WHERE note.parentId = :parentId AND " +
                        "note.parentType  = :parentType AND " +
                        "note.type = :type ORDER BY note.created DESC",
                Note.class);

        note.setParameter("type", type);
        note.setParameter("parentType", parentType.toUpperCase());
        note.setParameter("parentId", parentId);

        List<Note> notes = note.getResultList();
        if (null == notes)
        {
            notes = new ArrayList();
        }
        return notes;
    }

    public List<Note> listNotesPage(String type, Long parentId, String parentType, int start, int n, String sortParam)
    {
        // Preconditions.checkNotNull(type, "Note type cannot be null");
        Preconditions.checkNotNull(parentId, "Parent Id cannot be null");
        Preconditions.checkNotNull(parentType, "Parent type cannot be null");
        Preconditions.checkNotNull(start, "Start cannot be null");
        Preconditions.checkNotNull(n, "N cannot be null");

        String sortField = "created";
        String sortDirection = "DESC";

        if (StringUtils.isNotBlank(sortParam))
        {
            String[] parts = sortParam.split(" ");
            sortField = parts[0];
            if (parts.length == 2)
            {
                sortDirection = parts[1];
            }
        }

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Note> query = cb.createQuery(Note.class);
        Root<Note> note = query.from(Note.class);
        query.select(note);
        Predicate predicate = cb.and(cb.equal(note.get("parentId"), parentId), cb.equal(note.get("parentType"),
                parentType));
        if (type != null)
        {
            predicate = cb.and(predicate, cb.equal(note.get("type"), type));
        }

        query.where(predicate);
        if (sortDirection.equalsIgnoreCase("ASC"))
        {
            query.orderBy(cb.asc(note.get(sortField)));
        }
        else if (sortDirection.equalsIgnoreCase("DESC"))
        {
            query.orderBy(cb.desc(note.get(sortField)));
        }

        TypedQuery<Note> queryNotes = getEntityManager().createQuery(query);

        queryNotes.setFirstResult(start);
        queryNotes.setMaxResults(n);

        List<Note> notes = queryNotes.getResultList();
        if (null == notes)
        {
            notes = new ArrayList<>();
        }
        return notes;
    }

    public int countAll(String type, Long parentId, String parentType)
    {
        String queryText = "SELECT COUNT(note) " +
                "FROM Note note " +
                "WHERE note.parentId = :parentId AND note.parentType  = :parentType";

        if (type != null)
        {
            queryText += " AND note.type = :type";
        }

        TypedQuery<Long> query = getEm().createQuery(queryText, Long.class);

        if (type != null)
        {
            query.setParameter("type", type);
        }
        query.setParameter("parentType", parentType.toUpperCase());
        query.setParameter("parentId", parentId);

        Long count = 0L;

        try
        {
            count = query.getSingleResult();
        }
        catch (Exception e)
        {
            LOG.debug("There are no results.");
        }

        return count.intValue();
    }

    @Transactional
    public void deleteNoteById(Long id)
    {
        TypedQuery<Note> queryToDelete = getEntityManager().createQuery(
                "SELECT note " + "FROM Note note " +
                        "WHERE note.id = :noteId",
                Note.class);
        queryToDelete.setParameter("noteId", id);

        Note noteToBeDeleted = queryToDelete.getSingleResult();
        entityManager.remove(noteToBeDeleted);
    }

    public EntityManager getEntityManager()
    {
        return entityManager;
    }
}
