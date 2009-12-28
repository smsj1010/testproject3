package org.ozsoft.taskman.services;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.ozsoft.taskman.domain.Task;

/**
 * Task service implementation.
 * 
 * @author Oscar Stigter
 */
public class TaskServiceImpl implements TaskService {
    
    /** Entity manager. */
    private final EntityManager em;
    
    /**
     * Constructor.
     */
    public TaskServiceImpl() {
	em = PersistenceService.getEntityManager();
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.taskman.services.TaskService#create(org.ozsoft.taskman.domain.Task)
     */
    @Override
    public void create(Task task) {
	EntityTransaction tx = em.getTransaction();
	tx.begin();
	try {
	    em.persist(task);
	    tx.commit();
	} catch (PersistenceException e) {
	    tx.rollback();
	    throw e;
	}
    }
    
    /*
     * (non-Javadoc)
     * @see org.ozsoft.taskman.services.TaskService#retrieveById(long)
     */
    @Override
    public Task retrieveById(long id) {
	return em.find(Task.class, id);
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.taskman.services.TaskService#retrieveByUser(long)
     */
    @Override
    public List<Task> retrieveByUser(long userId) {
	return null;
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.taskman.services.TaskService#update(org.ozsoft.taskman.domain.Task)
     */
    @Override
    public void update(Task task) {
	EntityTransaction tx = em.getTransaction();
	tx.begin();
	try {
	    em.merge(task);
	    tx.commit();
	} catch (PersistenceException e) {
	    tx.rollback();
	    throw e;
	}
    }

}
