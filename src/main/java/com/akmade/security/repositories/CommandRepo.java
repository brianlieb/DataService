package com.akmade.security.repositories;

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
	
	public void saveAddressType(SecurityDTO.Type dto) {
		logger.info("Making Address Type for: " + dto.getType() + " " + dto.getDescription());
 		AddressRepo.persistAddressType.apply(dto).run(dataSource);
	}
	
	public void deleteAddressType(SecurityDTO.Type dto) {
		logger.info("Deleting Address Type for: " + dto.getType() + " " + dto.getDescription());
		AddressRepo.deleteAddressType.apply(dto).run(dataSource);
	}
					
	public void saveRoleType(SecurityDTO.Type dto) {
		logger.info("Making Role Type for: " + dto.getType() + " " + dto.getDescription());
 		RoleRepo.persistRoleType.apply(dto).run(dataSource);
	}
	
	public void deleteRoleType(SecurityDTO.Type dto) {
		logger.info("Deleting Role Type for: " + dto.getType() + " " + dto.getDescription());
		RoleRepo.deleteRoleType.apply(dto).run(dataSource);
	}

	public void savePhoneType(SecurityDTO.Type dto) {
		logger.info("Making Phone Type for: " + dto.getType() + " " + dto.getDescription());
		PhoneRepo.persistPhoneType.apply(dto).run(dataSource);
	}
	
	public void deletePhoneType(SecurityDTO.Type dto) {
		logger.info("Deleting Phone Type for: " + dto.getType() + " " + dto.getDescription());
		PhoneRepo.deletePhoneType.apply(dto).run(dataSource);
	}

	public void saveCompany(SecurityDTO.Company companyDTO) {
		logger.info("Saving the company: " + companyDTO.getCompany() + ".");
		CompanyRepo.persistCompanyTree.apply(companyDTO).run(dataSource);
	}
	
	public void saveUser(SecurityDTO.Account accountDTO) {
		logger.info("Saving the user: " + accountDTO.getUserName() + ".");
		UserRepo.persistUserTree.apply(accountDTO).run(dataSource);
	}


}
