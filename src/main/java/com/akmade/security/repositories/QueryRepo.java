package com.akmade.security.repositories;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import com.akmade.security.util.SessionUtility;
import com.akmade.security.util.HibernateSessionFactory.DataSource;
import com.akmade.messaging.security.dto.SecurityDTO;

public class QueryRepo extends SessionUtility {
	private DataSource dataSource = DataSource.DEFAULT;

	public QueryRepo() {
		super();
	}
	
	public QueryRepo(DataSource ds) {
		super();
		this.dataSource = ds;
	}
	
	public Supplier<Collection<SecurityDTO.Type>> getAddressTypes = 
			() -> AddressRepo.getAddressTypeDTOs.run(dataSource);

	public Function<String, Supplier<SecurityDTO.Account>> getAccountByUsername =
			username ->
				() -> UserRepo.getAccountDTOByUsername.apply(username).run(dataSource);
				
	public Supplier<Collection<SecurityDTO.Account>> getAccounts = 
			() -> UserRepo.getAccountDTOs.run(dataSource); 

	public Supplier<Collection<SecurityDTO.Type>> getRoleTypes = 
			() -> RoleRepo.getRoleTypeDTOS.run(dataSource);
			
	public Function<Integer, Supplier<SecurityDTO.Account>> getAccountById =
			userId ->
				() -> UserRepo.getAccountDTOById.apply(userId).run(dataSource);	 

	public Supplier<Collection<SecurityDTO.Type>> getPhoneTypes = 
			() -> PhoneRepo.getPhoneTypeDTOS.run(dataSource);

}
