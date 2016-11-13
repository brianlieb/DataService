package com.akmade.security.repositories;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.Session;

import com.akmade.security.data.HibernateSessionFactory.DataSource;
import com.akmade.security.dto.DataTransferObjects;

public class CommandRepo extends SessionRepo {
	private DataSource dataSource = DataSource.DEFAULT;

	public CommandRepo() {
		super();
	}
	
	private Consumer<Session> makeCommand(Function<Session, Consumer<Session>> fn) {
		Session session = createSession(dataSource);
		try {
			return fn.apply(session);
		} catch (Exception e) {
			rollback(session);
			throw logAndThrowError("Error creating the transactions." + e.getMessage());
		} finally {
			logger.info("made");
			endSession(session);
		}	
	}
	
	private void runCommand(Function<Session, Consumer<Session>> fn) {
		Consumer<Session> txn = makeCommand(fn);
		
		Session session = createSession(dataSource);
		logger.debug("Saving!");
		try {
			txn.accept(session);
		} catch (Exception e) {
			rollbackAndClose(session);
			throw logAndThrowError("Error running the transaction. " + e.getMessage());
		} finally {
			logger.debug("saved");
			endSession(session);
		}			
	}
	
	public void saveAddressType(ImmutablePair<String, String> dto) {
		logger.info("Making Address Type for: " + dto.left + " " + dto.right);
 		runCommand(AddressRepo.persistAddressType.apply(dto));
	}
	
	public void deleteAddressType(ImmutablePair<String, String> dto) {
		logger.info("Deleting Address Type for: " + dto.left + " " + dto.right);
		runCommand(AddressRepo.deleteAddressType.apply(dto));
	}
					
	public void saveRoleType(ImmutablePair<String, String> dto) {
		logger.info("Making Role Type for: " + dto.left + " " + dto.right);
 		runCommand(RoleRepo.persistRoleType.apply(dto));
	}
	
	public void deleteRoleType(ImmutablePair<String, String> dto) {
		logger.info("Deleting Role Type for: " + dto.left + " " + dto.right);
		runCommand(RoleRepo.deleteRoleType.apply(dto));
	}

	public void savePhoneType(ImmutablePair<String, String> dto) {
		logger.info("Making Phone Type for: " + dto.left + " " + dto.right);
		runCommand(PhoneRepo.persistPhoneType.apply(dto));
	}
	
	public void deletePhoneType(ImmutablePair<String, String> dto) {
		logger.info("Deleting Phone Type for: " + dto.left + " " + dto.right);
		runCommand(PhoneRepo.deletePhoneType.apply(dto));
	}

	public void saveCompany(DataTransferObjects.Company companyDTO) {
		logger.info("Saving the company: " + companyDTO.getCompany() + ".");
		runCommand(CompanyRepo.persistCompanyTree.apply(companyDTO));
	}
	
	public void saveUser(DataTransferObjects.Account accountDTO) {
		logger.info("Saving the user: " + accountDTO.getUserName() + ".");
		runCommand(UserRepo.persistUserTree.apply(accountDTO));
	}


}
