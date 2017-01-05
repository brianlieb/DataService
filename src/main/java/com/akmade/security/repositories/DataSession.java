package com.akmade.security.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akmade.exceptions.UnrecoverableException;

public class DataSession {
	protected static Logger logger = LoggerFactory.getLogger(DataSession.class);

	protected static UnrecoverableException logAndThrowError(String msg) throws UnrecoverableException {
		logger.error(msg);
		return new UnrecoverableException(msg);
	}

	protected static UnrecoverableException logAndThrowError(String msg, Exception e) throws UnrecoverableException {
		UnrecoverableException ex= new UnrecoverableException(msg + "\n" + e.getMessage());
		ex.addSuppressed(e);
		logger.error(msg, ex);
		return ex;
	}	

}
