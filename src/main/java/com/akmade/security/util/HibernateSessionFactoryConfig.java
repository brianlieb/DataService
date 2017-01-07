package com.akmade.security.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateSessionFactoryConfig {
	private static Logger logger = LoggerFactory.getLogger(HibernateSessionFactoryConfig.class);
	private SessionFactory sessionFactory = null;
	private ThreadLocal<Session> session = null;
	private String propertyFile = "hibernate.properties";
	private Properties properties = null;
    private static ServiceRegistry serviceRegistry;
	
	public HibernateSessionFactoryConfig(SessionFactory factory, ThreadLocal<Session> sess, String file) {
		sessionFactory = factory;
		session = sess;
		setPropertyFile(file);
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	public ThreadLocal<Session> getSession() {
		return session;
	}
	public void setSession(ThreadLocal<Session> session) {
		this.session = session;
	}
	public String getPropertyFile() {
		return propertyFile;
	}
	public void setPropertyFile(String propertyFile) {
    	if (doesResourceExist(propertyFile)) {
    		this.propertyFile = propertyFile;    		
    	}
		setProperties();
	}
	
	private void setProperties() {
		Properties prop = new Properties();
    	ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try(InputStream is = loader.getResourceAsStream(propertyFile)) {
			if (is != null) {
				prop.load(is);
			} else {
				logger.error("Failed loading properties: " + propertyFile);
			}
		} catch (Exception e) {
			logger.error("Failed loading properties: " + propertyFile, e);
		}
		this.properties  = prop;
	}
	
	/**
	 * Tests if a resource can be found using getResourceAsStream(),
	 * which is how hibernate's Configuration.configure() finds
	 * the config resource.
	 */
	private boolean doesResourceExist(String resourceName) {
		InputStream stream = HibernateSessionFactory.class.getClassLoader().getResourceAsStream(resourceName);
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
		return stream != null;
	}

	public void rebuildSessionFactory() {
		try {
			Configuration configuration = new Configuration();
			configuration = configuration.setProperties(properties);
			addAnnotatedHibernateEntitiesToConfiguration(configuration);
			serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
			this.setSessionFactory(configuration.buildSessionFactory(serviceRegistry));
			logConfiguration(configuration);
		} catch (Exception ex) {
			logger.error("Error Creating SessionFactory", ex);
		}
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	/**
	 * Add all annotated hibernate entity classes from all 
	 * packages to the configuration using reflection.
	 * 
	 * Reflection is used to find all classes with the prefix
	 * "gov.noaa.alaskafisheries" and the Entity annotation
	 * in all packages. This allows CAS2_Library's session
	 * factory to use entities that live outside of it and
	 * don't appear in its hibernate.cfg.xml. It also makes
	 * the CAS2_Library hibernate.cfg.xml unnecessary.
	 */
	private static void addAnnotatedHibernateEntitiesToConfiguration(Configuration configuration) {
		
		// this reflection query will also return CAS2_Library's entities
		//  which may have already been added to the configuration through
		//  hibernate.cfg.xml. that's okay, Configuration.buildSessionFactory()
		//  will flatten any duplicate mappings.
		Reflections reflections = new Reflections("com.akmade.security.data");
		Set<Class<?>> entities = reflections.getTypesAnnotatedWith(javax.persistence.Entity.class);
		for (Class<?> entity : entities) {
			logger.debug("Adding hibernate entity: " + entity + " - " + entity.getProtectionDomain().getCodeSource().getLocation().getPath()  );
			configuration.addAnnotatedClass(entity);
		}
		logger.info("Annotated hibernate entities added: " + entities.size());
	}
	
	
    
    /**
     * Log configuration.
     */
	private static void logConfiguration(Configuration configuration)
	{
		logger.info("Hibernate Version: " + org.hibernate.Version.getVersionString());
		Set<String> keys = configuration.getProperties().stringPropertyNames();
		for (String key : keys) {
			if(key.startsWith("hibernate") && !key.contains("password")) {
				logger.info(key + ": " + configuration.getProperties().getProperty(key));
			}
		}
    }
	

	
}