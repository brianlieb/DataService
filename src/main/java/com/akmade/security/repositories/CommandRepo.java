package com.akmade.security.repositories;

import com.akmade.security.util.Transaction;
import com.akmade.security.util.HibernateSessionFactory.DataSource;

import static com.akmade.security.util.RepositoryUtility.*;

import com.akmade.messaging.security.dto.SecurityDTO;

public class CommandRepo {
	private DataSource dataSource = DataSource.DEFAULT;

	public CommandRepo() {
		super();
	}
	
	public CommandRepo(DataSource ds) {
		super();
		this.dataSource = ds;
	}
	
	public void saveAddressType(SecurityDTO.Type dto) {
		logger.info("Making Address Type for: " + dto.getType() + " " + dto.getDescription());
		new Transaction()
				.addTxn(AddressRepo.persistAddressType.apply(dto))
				.run(dataSource);
	}
	
	public void deleteAddressType(SecurityDTO.Type dto) {
		logger.info("Deleting Address Type for: " + dto.getType() + " " + dto.getDescription());
		new Transaction()
			.addTxn(AddressRepo.deleteAddressType.apply(dto))
			.run(dataSource);
	}
					
	public void saveRoleType(SecurityDTO.Type dto) {
		logger.info("Making Role Type for: " + dto.getType() + " " + dto.getDescription());
		new Transaction()
			.addTxn(RoleRepo.persistRoleType.apply(dto))
			.run(dataSource);
	}
	
	public void deleteRoleType(SecurityDTO.Type dto) {
		logger.info("Deleting Role Type for: " + dto.getType() + " " + dto.getDescription());
		new Transaction()
			.addTxn(RoleRepo.deleteRoleType.apply(dto))
			.run(dataSource);
	}

	public void savePhoneType(SecurityDTO.Type dto) {
		logger.info("Making Phone Type for: " + dto.getType() + " " + dto.getDescription());
		new Transaction()
			.addTxn(PhoneRepo.persistPhoneType.apply(dto))
			.run(dataSource);
	}
	
	public void deletePhoneType(SecurityDTO.Type dto) {
		logger.info("Deleting Phone Type for: " + dto.getType() + " " + dto.getDescription());
		new Transaction()
			.addTxn(PhoneRepo.deletePhoneType.apply(dto))
			.run(dataSource);
	}

	public void saveCompany(SecurityDTO.Company companyDTO) {
		logger.info("Saving the company: " + companyDTO.getCompany() + ".");
		new Transaction()
			.addTxn(CompanyRepo.persistCompanyTree.apply(companyDTO))
			.run(dataSource);
	}
	
	public void saveUser(SecurityDTO.Account accountDTO) {
		logger.info("Saving the user: " + accountDTO.getUserName() + ".");
		new Transaction()
			.addTxn(UserRepo.persistUserTree.apply(accountDTO))
			.run(dataSource);
	}


}
