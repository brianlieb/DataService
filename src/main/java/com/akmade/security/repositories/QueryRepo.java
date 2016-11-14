package com.akmade.security.repositories;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.Session;

import com.akmade.security.data.HibernateSessionFactory.DataSource;
import com.akmade.messaging.security.dto.SecurityDTO;

public class QueryRepo extends SessionRepo {
	private DataSource dataSource = DataSource.DEFAULT;

	public QueryRepo() {
		super();
	}
	
	public QueryRepo(DataSource ds) {
		super();
		this.dataSource = ds;
	}
	
	private <T> T runQuery(Function<Session, T> q) {
		Session session = createSession(dataSource);
		logger.debug("Running query."); 
		try {
			return q.apply(session);
		} catch (Exception e) {
			rollbackAndClose(session);
			throw logAndThrowError("Error running the query. " + e.getMessage());
		} finally {
			endSession(session);
		}
	}

	public Supplier<Collection<SecurityDTO.Type>> getAddressTypes = 
			() -> runQuery(AddressRepo.getAddressTypeDTOs);

	public Function<String, Supplier<SecurityDTO.Account>> getAccountByUsername =
			username ->
				() -> runQuery(UserRepo.getAccountDTOByUsername.apply(username));
				
	public Supplier<Collection<SecurityDTO.Account>> getAccounts = 
			() -> runQuery(UserRepo.getAccountDTOs); 

	public Supplier<Collection<SecurityDTO.Type>> getRoleTypes = 
			() -> runQuery(RoleRepo.getRoleTypeDTOS);
			
	public Function<Integer, Supplier<SecurityDTO.Account>> getAccountById =
			userId ->
				() -> runQuery(UserRepo.getAccountDTOById.apply(userId));	 

	public Supplier<Collection<SecurityDTO.Type>> getPhoneTypes = 
			() -> runQuery(PhoneRepo.getPhoneTypeDTOS);

}
