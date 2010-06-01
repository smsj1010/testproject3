package sr.projectx.services;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import sr.projectx.entities.User;

/**
 * User service implementation.
 * 
 * @author Oscar Stigter
 */
public class UserServiceImpl implements UserService {
    
    /** Persistence unit name. */
    private static final String PU_NAME = "projectx";

	/** Log. */
	private static final Logger LOG = Logger.getLogger(UserServiceImpl.class);

	/** Entity manager. */
	private EntityManager em;
	
	public UserServiceImpl() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PU_NAME);
        em = emf.createEntityManager();
	}

    /*
     * (non-Javadoc)
     * @see sr.projectx.services.UserService#create(sr.projectx.entities.User)
     */
    public void create(User user) throws PersistenceException {
        String username = user.getUsername();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
    		em.persist(user);
    		tx.commit();
    		LOG.debug(String.format("Created user '%s'", username));
        } catch (Exception e) {
            tx.rollback();
            String msg = String.format("Could not create user '%s'", username); 
            throw new PersistenceException(msg, e);
        }
	}
	
	/*
	 * (non-Javadoc)
	 * @see sr.projectx.services.UserService#retrieve(long)
	 */
    @Override
	public User retrieve(long id) {
		return em.find(User.class, id);
	}

	/*
	 * (non-Javadoc)
	 * @see sr.projectx.services.UserService#retrieve(java.lang.String)
	 */
    @Override
	public User retrieve(String username) {
		User user = null;
		Query query = em.createQuery("SELECT u FROM USERS u WHERE u.username = :username");
		query.setParameter("username", username);
		try {
			user = (User) query.getSingleResult();
		} catch (NoResultException e) {
			// Ignore.
		}
		return user;
	}

	/*
	 * (non-Javadoc)
	 * @see sr.projectx.services.UserService#update(sr.projectx.entities.User)
	 */
    @Override
	public void update(User user) throws PersistenceException {
        String username = user.getUsername();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
    		em.merge(user);
            tx.commit();
    		LOG.debug(String.format("Updated user '%s'", user.getUsername()));
        } catch (Exception e) {
            tx.rollback();
            String msg = String.format("Could not update user '%s'", username); 
            throw new PersistenceException(msg, e);
        }
	}

	/*
	 * (non-Javadoc)
	 * @see sr.projectx.services.UserService#delete(sr.projectx.entities.User)
	 */
    @Override
	public void delete(User user) throws PersistenceException {
		String username = user.getUsername();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
    		em.remove(user);
            tx.commit();
    		LOG.debug(String.format("Deleted user '%s'", username));
        } catch (Exception e) {
            tx.rollback();
            String msg = String.format("Could not delete user '%s'", username); 
            throw new PersistenceException(msg, e);
        }
	}

	/*
	 * (non-Javadoc)
	 * @see sr.projectx.services.UserService#checkCredentials(java.lang.String, java.lang.String)
	 */
    @Override
	public boolean checkCredentials(String username, String password) {
		boolean valid = false;
		User user = retrieve(username);
		if (user != null && user.getPassword().equals(password)) {
			valid = true;
		}
		return valid;
	}
	
}