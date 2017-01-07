package com.akmade.security.util;

import java.util.Objects;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akmade.security.util.HibernateSessionFactory.DataSource;

public class Transaction extends SessionUtility {
	private static Logger logger = LoggerFactory.getLogger(Transaction.class);
	private Txn transaction = (s) -> {};
	
	public Transaction() {
	}
	
	public Transaction addTxn(Txn transaction) {
		this.transaction = this.transaction.andThen(transaction);
		return this;
	}
	
	public void run(DataSource ds) {
		Session session = createSession(ds);
		logger.info("Beginning Transaction!");
		try {
			this.transaction.execute(session);
		} catch (Exception e) {
			rollbackAndClose(session);
			throw logAndThrowError("Error running the transaction. " + e.getMessage());
		} finally {
			logger.info("Transaction Committed.");
			endSession(session);
		}
	}
	
	@FunctionalInterface
	public interface Txn {
		
	    void execute(Session s);

	    default Txn andThen(Txn after) {
	        Objects.requireNonNull(after);
	        return (session) -> {
	        	execute(session); 
	        	logger.info("---");
	        	after.execute(session); 
	        };
	    }
	    
	}
	
}
