package com.akmade.security.repositories;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import com.akmade.security.dto.DataTransferObjects;

public class QueryRepo extends SessionRepo {
	
	public QueryRepo() {
		super();
	}

	public Supplier<Collection<String>> getAddressTypes = 
			() -> runQuery(AddressRepo.getAddressTypeDTOs);

	public Function<String, Supplier<DataTransferObjects.Account>> getAccountByUsername =
			username ->
				() -> runQuery(UserRepo.getAccountDTOByUsername.apply(username));
				
	public Supplier<Collection<DataTransferObjects.Account>> getAccounts = 
			() -> runQuery(UserRepo.getAccountDTOs); 

	public Supplier<Collection<String>> getRoleTypes = 
			() -> runQuery(RoleRepo.getRoleTypeDTOS);
			
	public Function<Integer, Supplier<DataTransferObjects.Account>> getAccountById =
			userId ->
				() -> runQuery(UserRepo.getAccountDTOById.apply(userId));	 

	public Supplier<Collection<String>> getPhoneTypes = 
			() -> runQuery(PhoneRepo.getPhoneTypeDTOS);

}
