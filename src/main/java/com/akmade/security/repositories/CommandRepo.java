package com.akmade.security.repositories;

import org.hibernate.Session;

import com.akmade.security.data.HibernateSessionFactory.DataSource;
import com.akmade.messaging.security.dto.SecurityDTO;

public class CommandRepo extends SessionRepo {
	private DataSource dataSource = DataSource.DEFAULT;

	public CommandRepo() {
		super();
	}
	
	public CommandRepo(DataSource ds) {
		super();
		this.dataSource = ds;
	}
	
	private void runCommand(Txn txn) {
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
	
	public void saveAddressType(SecurityDTO.Type dto) {
		logger.info("Making Address Type for: " + dto.getType() + " " + dto.getDescription());
 		runCommand(AddressRepo.persistAddressType.apply(dto));
	}
	
	public void deleteAddressType(SecurityDTO.Type dto) {
		logger.info("Deleting Address Type for: " + dto.getType() + " " + dto.getDescription());
		runCommand(AddressRepo.deleteAddressType.apply(dto));
	}
					
	public void saveRoleType(SecurityDTO.Type dto) {
		logger.info("Making Role Type for: " + dto.getType() + " " + dto.getDescription());
 		runCommand(RoleRepo.persistRoleType.apply(dto));
	}
	
	public void deleteRoleType(SecurityDTO.Type dto) {
		logger.info("Deleting Role Type for: " + dto.getType() + " " + dto.getDescription());
		runCommand(RoleRepo.deleteRoleType.apply(dto));
	}

	public void savePhoneType(SecurityDTO.Type dto) {
		logger.info("Making Phone Type for: " + dto.getType() + " " + dto.getDescription());
		runCommand(PhoneRepo.persistPhoneType.apply(dto));
	}
	
	public void deletePhoneType(SecurityDTO.Type dto) {
		logger.info("Deleting Phone Type for: " + dto.getType() + " " + dto.getDescription());
		runCommand(PhoneRepo.deletePhoneType.apply(dto));
	}

	public void saveCompany(SecurityDTO.Company companyDTO) {
		logger.info("Saving the company: " + companyDTO.getCompany() + ".");
		runCommand(CompanyRepo.persistCompanyTree.apply(companyDTO));
	}
	
	public void saveUser(SecurityDTO.Account accountDTO) {
		logger.info("Saving the user: " + accountDTO.getUserName() + ".");
		runCommand(UserRepo.persistUserTree.apply(accountDTO));
	}


}
