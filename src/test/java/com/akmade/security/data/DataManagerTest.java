package com.akmade.security.data;

import static org.junit.Assert.*;

import java.util.Collection;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.akmade.messaging.security.dto.SecurityDTO;
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
		cmd.saveAddressType(SecurityDTO.Type.newBuilder().setType("Home").setDescription("This is where you live.").build());
		cmd.saveAddressType(SecurityDTO.Type.newBuilder().setType("Billing").setDescription("This is where you receive your credit card bills.").build());
		cmd.saveAddressType(SecurityDTO.Type.newBuilder().setType("Shipping").setDescription("This is where your receive packages.").build());
		
		Collection<SecurityDTO.Type> types = qry.getAddressTypes.get();
		assertTrue(types.size() == 3);
	}

}
