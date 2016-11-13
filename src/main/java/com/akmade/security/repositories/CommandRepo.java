package com.akmade.security.repositories;

import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.Session;

import com.akmade.security.dto.DataTransferObjects;

public class CommandRepo extends SessionRepo {
	
	public CommandRepo() {
		super();
	}
	
	public void saveAddressType(ImmutablePair<String, String> dto) {
		logger.info("Making Address Type for: " + dto.left + " " + dto.right);
 		Consumer<Session> txn = makeTransaction(AddressRepo.persistAddressType.apply(dto), "Address Type");
		runTransaction(txn);
	}
	
	public void deleteAddressType(ImmutablePair<String, String> dto) {
		logger.info("Deleting Address Type for: " + dto.left + " " + dto.right);
		Consumer<Session> txn = makeTransaction(AddressRepo.deleteAddressType.apply(dto), "Address Type");
		runTransaction(txn);
	}
					
	public void saveRoleType(ImmutablePair<String, String> dto) {
		logger.info("Making Role Type for: " + dto.left + " " + dto.right);
 		Consumer<Session> txn = makeTransaction(RoleRepo.persistRoleType.apply(dto), "Role Type");
		runTransaction(txn);
	}
	
	public void deleteRoleType(ImmutablePair<String, String> dto) {
		logger.info("Deleting Role Type for: " + dto.left + " " + dto.right);
		Consumer<Session> txn = makeTransaction(RoleRepo.deleteRoleType.apply(dto), "Role Type");
		runTransaction(txn);
	}

	public void savePhoneType(ImmutablePair<String, String> dto) {
		logger.info("Making Phone Type for: " + dto.left + " " + dto.right);
 		Consumer<Session> txn = makeTransaction(PhoneRepo.persistPhoneType.apply(dto), "Phone Type");
		runTransaction(txn);
	}
	
	public void deletePhoneType(ImmutablePair<String, String> dto) {
		logger.info("Deleting Phone Type for: " + dto.left + " " + dto.right);
		Consumer<Session> txn = makeTransaction(PhoneRepo.deletePhoneType.apply(dto), "Phone Type");
		runTransaction(txn);
	}

	public void saveCompany(DataTransferObjects.Company companyDTO) {
		logger.info("Saving the company: " + companyDTO.getCompany() + ".");
		Consumer<Session> txn = makeTransaction(CompanyRepo.persistCompanyTree.apply(companyDTO), "companies");
		runTransaction(txn);
	}
	
	public void saveUser(DataTransferObjects.Account accountDTO) {
		logger.info("Saving the user: " + accountDTO.getUserName() + ".");
		Consumer<Session> txn = makeTransaction(UserRepo.persistUserTree.apply(accountDTO), "users");
		runTransaction(txn);
	}


}
