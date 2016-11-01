package com.akmade.security.data;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataManagerTest {
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	

	@Test
	public void testSaveAddressTypes() {
		SecurityCommandRepository cmdRepo = new SecurityCommandRepository();
		cmdRepo.saveAddressType(new ImmutablePair<String, String>("Home", "This is where you live."));
		cmdRepo.saveAddressType(new ImmutablePair<String, String>("Billing", "This is where you receive your credit card statements."));
		cmdRepo.saveAddressType(new ImmutablePair<String, String>("Shipping", "This is where you receive shipments."));
		
		SecurityQueryRepository qryRepo = new SecurityQueryRepository();
		Collection<String> types = qryRepo.getAddressTypes();
		assertTrue(types.size() == 3);
	}

}
