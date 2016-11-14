package com.akmade.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

	public static void main(String[] args) throws Exception {
		Logger log = LoggerFactory.getLogger(App.class);
    	log.info("Starting up Demo Person Data Async Service");
	        new SecurityData();
	}

}
