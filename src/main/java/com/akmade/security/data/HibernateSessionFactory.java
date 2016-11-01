package com.akmade.security.data;



import java.util.HashMap;
import java.util.Map;
import org.hibernate.HibernateException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

import com.akmade.exceptions.UnrecoverableException;

/**
 * Configures and provides access to Hibernate sessions, tied to the
 * current thread of execution.  Follows the Thread Local Session
 * pattern, see http://hibernate.org/42.html
 */
public class HibernateSessionFactory {
    public enum DataSource
    {
    	DEFAULT
    }
    
    private Map<DataSource, HibernateSessionFactoryConfig> sessionFactories = new HashMap<>(); 
    private static HibernateSessionFactory instance;

    
    /**
     * Instantiates a new hibernate session factory.
     */
    private HibernateSessionFactory() {
    	initSessionFactories();
    }

    public static HibernateSessionFactory getInstance() {
		if(instance == null){
			synchronized(HibernateSessionFactory.class)
			{
				instance = new HibernateSessionFactory();
			}
        }
        return instance;
    }
    
	/**
	 * Get the name of the primary key column for the provided hibernate class
	 * 
	 * @param clzz the hibernate class 
	 * @return The name of the primary key column
	 */
	public <T> String getIdentifyingPropertyName(Class<T> clzz){
		return getIdentifyingPropertyName(clzz, DataSource.DEFAULT);
	}
	
	
	public <T> String getIdentifyingPropertyName(Class<T> clzz, DataSource ds){
		return sessionFactories.get(ds).getSessionFactory().getClassMetadata(clzz).getIdentifierPropertyName();
	}

    
    
    
    private void initSessionFactories() {
    	sessionFactories.put(DataSource.DEFAULT, new HibernateSessionFactoryConfig(null, new ThreadLocal<Session>(), "hibernate.properties"));
		sessionFactories.get(DataSource.DEFAULT).rebuildSessionFactory();
    }
    
    /**
     * 
     * @deprecated This method has been replaced by getSession(HibernateSessionFactory.DataSource)
     */
    @Deprecated 
	public Session getSession() {
		return getSession(DataSource.DEFAULT);
	}
    
	/**
	 * Returns the ThreadLocal Session instance.  Lazy initialize
	 * the <code>SessionFactory</code> if needed.
	 *
	 * @return Session
	 * @throws CasUnrecoverableException the caught hibernate exception
	 */
    public Session getSession(DataSource ds)  {
    	ThreadLocal<Session> threadLocal = sessionFactories.get(ds).getSession();
		if (threadLocal.get() == null || ! threadLocal.get().isOpen())
			threadLocal.set(createSession(ds));
		return threadLocal.get();
    }
    
    /**
     * 
     * @deprecated This method has been replaced by createSession(HibernateSessionFactory.DataSource)
     */
    @Deprecated
    public Session createSession() {
    	return createSession(DataSource.DEFAULT);
    }
    
    /**
     * Creates a new Hibernate Session
     * 
     * @return a new Hibernate Session
     */
    public Session createSession(DataSource ds){
    	Session session = null;
    	HibernateSessionFactoryConfig sessConfig = sessionFactories.get(ds);
    	try {
			if (sessConfig.getSessionFactory() == null) 
				sessConfig.rebuildSessionFactory();
			session = (sessConfig.getSessionFactory() != null) ? sessConfig.getSessionFactory().openSession() : null;
        }catch(HibernateException he){
        	UnrecoverableException cue = new UnrecoverableException(he.toString());
        	cue.addSuppressed(he);
        	throw cue;
        }
        return session;
    }
    
    public StatelessSession createStatelessSession(DataSource ds){
    	StatelessSession session = null;
    	HibernateSessionFactoryConfig sessConfig = sessionFactories.get(ds);
    	try {
			if (sessConfig.getSessionFactory() == null) 
				sessConfig.rebuildSessionFactory();
			session = (sessConfig.getSessionFactory() != null) ? sessConfig.getSessionFactory().openStatelessSession() : null;
        }catch(HibernateException he){
        	UnrecoverableException cue = new UnrecoverableException(he.toString());
        	cue.addSuppressed(he);
        	throw cue;
        }
        return session;
    }
    
    /**
     * Closes the session factory
     */
    public void closeSessionFactory(DataSource ds){
    	SessionFactory sessionFactory = sessionFactories.get(ds).getSessionFactory();
    	if (sessionFactory != null)
    		sessionFactory.close();
    }
    
    /**
     * 
     * @deprecated This method has been replaced by closeSessionFactory(HibernateSessionFactory.DataSource)
     */
    @Deprecated
    public void closeSessionFactory() {
    	closeSessionFactory(DataSource.DEFAULT);
    }
     
    public void closeSessionFactories() {
    	sessionFactories.keySet().parallelStream().forEach(ds -> closeSessionFactory(ds));
    }
}