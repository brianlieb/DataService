package com.akmade.security.util;

import java.util.function.Function;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akmade.exceptions.UnrecoverableException;
import com.akmade.security.util.HibernateSessionFactory.DataSource;

public class SessionUtility {
	protected static Logger logger = LoggerFactory.getLogger(SessionUtility.class);


	public SessionUtility () {
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
	public interface CritQuery extends Function<Session, Criteria>{
	}
	
}
