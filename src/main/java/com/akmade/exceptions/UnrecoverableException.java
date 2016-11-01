package com.akmade.exceptions;

@SuppressWarnings("serial")
public class UnrecoverableException extends RuntimeException 
{

	/**
	* Instantiates a new exception.
	*
	* @param msg the msg
	*/
	public UnrecoverableException(String msg) {
		super(msg);
	}
	
	/**
	* @param e the throwable
	*/
	public UnrecoverableException(Throwable e) {
		super(e);
	}
}
