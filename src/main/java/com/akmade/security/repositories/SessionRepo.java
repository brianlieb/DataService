package com.akmade.security.repositories;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akmade.exceptions.UnrecoverableException;
import com.akmade.security.data.HibernateSessionFactory;
import com.akmade.security.data.HibernateSessionFactory.DataSource;

public class SessionRepo {
	protected DataSource dataSource = DataSource.DEFAULT;
	protected static Logger logger = LoggerFactory.getLogger(SessionRepo.class);


	public SessionRepo () {
	}
	
	public SessionRepo(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	protected Session createSession() {
		return createSession(dataSource);
	}

	protected Session createSession(DataSource ds) {
		Session session = HibernateSessionFactory.getInstance().createSession(ds);
		session.setFlushMode(FlushMode.MANUAL);
		session.beginTransaction();
		return session;
	}

	protected void rollbackAndClose(Session session){
		rollback(session);
		session.clear();
		session.close();
	}
	
	protected void rollback(Session session) {
		if(TransactionStatus.ACTIVE.equals(session.getTransaction().getStatus())){
			session.getTransaction().rollback();			
		}
	}
	
	protected void commit(Session session) {
		if (session != null && session.isOpen()) {
			if(TransactionStatus.ACTIVE.equals(session.getTransaction().getStatus())){
				session.flush();
				session.getTransaction().commit();
			}
		}
	}
	
	protected void commitAndClose(Session session){
		if (session != null && session.isOpen()) {
			commit(session);
			session.clear();
			session.close();
		}
	}
		
	protected void endSession(Session session){
		commitAndClose(session);
	}
	
	protected void beginTransaction(Session session) {
		if ((session.getTransaction() == null)||
			(! TransactionStatus.ACTIVE.equals(session.getTransaction().getStatus())))
			session.beginTransaction();
	}

	protected UnrecoverableException logAndThrowError(String msg) throws UnrecoverableException {
		logger.error(msg);
		return new UnrecoverableException(msg);
	}

	protected UnrecoverableException logAndThrowError(String msg, Exception e) throws UnrecoverableException {
		UnrecoverableException ex= new UnrecoverableException(msg + "\n" + e.getMessage());
		ex.addSuppressed(e);
		logger.error(msg, ex);
		return ex;
	}	
	
	protected Consumer<Session> makeTransaction(Function<Session, Consumer<Session>> fn, String msg) {
		logger.info("Creating the transaction for " + msg +".");
		Session session = createSession(dataSource);
		try {
			return fn.apply(session);
		} catch (Exception e) {
			rollback(session);
			throw logAndThrowError("Error creating the transactions." + e.getMessage());
		} finally {
			logger.info("made");
			endSession(session);
		}	
	}
	
	protected <T> Object makeHibernate(Function<Session, T> fn, String objectType) {
		Session session = createSession(dataSource);
		logger.info("Making hibernate object " + objectType);
		try {
			return fn.apply(session);
		} catch (Exception e) {
			rollback(session);
			throw logAndThrowError("Error creating the " + objectType +"." + e.getMessage());
		} finally {
			logger.info("made");
			endSession(session);
		}
	}
	
	protected void runTransaction(Consumer<Session> c) {
		Session session = createSession(dataSource);
		logger.debug("Saving!");
		try {
			c.accept(session);
		} catch (Exception e) {
			rollbackAndClose(session);
			throw logAndThrowError("Error running the transaction. " + e.getMessage());
		} finally {
			logger.debug("saved");
			endSession(session);
		}			
	}
	
	
	protected <T> Collection<T> runQuerys(Function<Session, Collection<T>> q) {
		Session session = createSession(dataSource);
		logger.debug("Running query."); 
		try {
			return q.apply(session);
		} catch (Exception e) {
			rollbackAndClose(session);
			throw logAndThrowError("Error running the query. " + e.getMessage());
		} finally {
			endSession(session);
		}
	}
	
	protected <T> T runQuery(Function<Session, T> q) {
		Session session = createSession(dataSource);
		logger.debug("Running query."); 
		try {
			return q.apply(session);
		} catch (Exception e) {
			rollbackAndClose(session);
			throw logAndThrowError("Error running the query. " + e.getMessage());
		} finally {
			endSession(session);
		}
	}
	
}
