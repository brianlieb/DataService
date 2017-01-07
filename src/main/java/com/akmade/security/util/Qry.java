package com.akmade.security.util;

import static com.akmade.security.util.SessionUtility.*;

import org.hibernate.Session;

import com.akmade.security.util.HibernateSessionFactory.DataSource;

public interface Qry<T> {
	T execute(Session s);
	
	default T run(DataSource ds) {
		Session session = createSession(ds);
		logger.debug("Running query."); 
		try {
			return this.execute(session);
		} catch (Exception e) {
			rollbackAndClose(session);
			throw logAndThrowError("Error running the query. " + e.getMessage());
		} finally {
			endSession(session);
		}
	}
}
