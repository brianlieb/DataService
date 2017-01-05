package com.akmade.security.repositories;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akmade.exceptions.UnrecoverableException;
import com.akmade.security.data.HibernateSessionFactory;
import com.akmade.security.data.HibernateSessionFactory.DataSource;

public class SessionRepo {
	protected static Logger logger = LoggerFactory.getLogger(SessionRepo.class);


	public SessionRepo () {
	}
	
	protected static Session createSession(DataSource ds) {
		Session session = HibernateSessionFactory.getInstance().createSession(ds);
		session.setFlushMode(FlushMode.MANUAL);
		session.beginTransaction();
		return session;
	}

	protected static void rollbackAndClose(Session session){
		rollback(session);
		session.clear();
		session.close();
	}
	
	protected static void rollback(Session session) {
		if(TransactionStatus.ACTIVE.equals(session.getTransaction().getStatus())){
			session.getTransaction().rollback();			
		}
	}
	
	protected static void commit(Session session) {
		if (session != null && session.isOpen()) {
			if(TransactionStatus.ACTIVE.equals(session.getTransaction().getStatus())){
				session.flush();
				session.getTransaction().commit();
			}
		}
	}
	
	protected static void commitAndClose(Session session){
		if (session != null && session.isOpen()) {
			commit(session);
			session.clear();
			session.close();
		}
	}
		
	protected static void endSession(Session session){
		commitAndClose(session);
	}
	
	protected void beginTransaction(Session session) {
		if ((session.getTransaction() == null)||
			(! TransactionStatus.ACTIVE.equals(session.getTransaction().getStatus())))
			session.beginTransaction();
	}

	protected static UnrecoverableException logAndThrowError(String msg) throws UnrecoverableException {
		logger.error(msg);
		return new UnrecoverableException(msg);
	}

	protected UnrecoverableException logAndThrowError(String msg, Exception e) throws UnrecoverableException {
		UnrecoverableException ex= new UnrecoverableException(msg + "\n" + e.getMessage());
		ex.addSuppressed(e);
		logger.error(msg, ex);
		return ex;
	}	
	
	
	@FunctionalInterface
	public interface Txn {
		
	    void execute(Session s);
		
		
	    default Txn andThen(Txn after) {
	        Objects.requireNonNull(after);
	        return (session) -> { execute(session); after.execute(session); };
	    }
	    
		default void run(DataSource ds) {
			Session session = createSession(ds);
			logger.debug("Saving!");
			try {
				this.execute(session);
			} catch (Exception e) {
				rollbackAndClose(session);
				throw logAndThrowError("Error running the transaction. " + e.getMessage());
			} finally {
				logger.debug("saved");
				endSession(session);
			}			
		}
	}
	
	@FunctionalInterface
	public interface CritQuery extends Function<Session, Criteria>{
	}
	
	@FunctionalInterface
	public interface Qry<T> extends Function<Session, T>{
		default T run(DataSource ds) {
			Session session = createSession(ds);
			logger.debug("Running query."); 
			try {
				return this.apply(session);
			} catch (Exception e) {
				rollbackAndClose(session);
				throw logAndThrowError("Error running the query. " + e.getMessage());
			} finally {
				endSession(session);
			}
		}
	}
	
}
