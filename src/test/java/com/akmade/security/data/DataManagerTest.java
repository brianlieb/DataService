package com.akmade.security.data;

import static org.junit.Assert.*;

import java.util.Collection;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.akmade.security.repositories.CommandRepo;
import com.akmade.security.repositories.QueryRepo;

public class DataManagerTest {
	CommandRepo cmd;
	QueryRepo qry;
	
	@Before
	public void setUp() throws Exception {
		cmd = new CommandRepo();
		qry = new QueryRepo();
	}

	@After
	public void tearDown() throws Exception {
	}
	

	@Test
	public void testSaveAddressTypes() {
		cmd.saveAddressType(new ImmutablePair<String, String>("Home", "This is where you live."));
		cmd.saveAddressType(new ImmutablePair<String, String>("Billing", "This is where you receive your credit card bills."));
		cmd.saveAddressType(new ImmutablePair<String, String>("Shipping", "This is where your receive packages."));
		
		Collection<String> types = qry.getAddressTypes.get();
		assertTrue(types.size() == 3);
	}

}
