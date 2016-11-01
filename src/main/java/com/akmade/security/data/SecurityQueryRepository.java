package com.akmade.security.data;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.Session;

import com.akmade.security.data.Address;
import com.akmade.security.data.Company;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.CompanyPhone;
import com.akmade.security.data.Phone;
import com.akmade.security.data.Role;
import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
import com.akmade.security.data.UserRole;
import com.akmade.security.vos.AccountDTO;
import com.akmade.security.vos.AddressDTO;
import com.akmade.security.vos.CompanyDTO;
import com.akmade.security.vos.RoleDTO;
import static com.akmade.security.Constants.*;

public class SecurityQueryRepository extends SecuritySessionRepository {

	private Function<CompanyAddress, AddressDTO> makeNewCompanyAddressDTO =
			address -> new AddressDTO(address.getAddress1(), 
										address.getAddress2(),
										address.getCity(),
										address.getState(),
										address.getCountry(),
										address.getPostalCode()); 
	
	private Function<Address, AddressDTO> makeNewAddressDTO =
			address -> new AddressDTO(address.getAddress1(), 
										address.getAddress2(),
										address.getCity(),
										address.getState(),
										address.getCountry(),
										address.getPostalCode()); 
			
	private Function<Role, RoleDTO> makeNewRoleDTO = 
			role -> new RoleDTO(role.getRole(), role.getRoleType().getType(), role.getDescription());
			
	private Function<String, Function<Collection<Address>, AddressDTO>> makeNewAddressDTOByType =
			type ->
				coll -> coll.parallelStream()
							.filter(a -> a.getAddressType().getType().equals(type))
							.map(a -> makeNewAddressDTO.apply(a))
							.findFirst()
							.orElse(null);
			
	private Function<String, Function<Collection<Phone>, String>> makeNewPhoneDTOByType = 
			type ->
				coll -> coll.parallelStream()
							.filter(p -> p.getPhoneType().equals(type))
							.map(Phone::getPhone)
							.findFirst()
							.orElse(null);
				
	private Function<Collection<UserRole>, Collection<RoleDTO>> makeNewRoleDTOs =
				userRoles -> userRoles
								.parallelStream()
								.map(ur -> makeNewRoleDTO.apply(ur.getRole()))
								.collect(Collectors.toList());
				
	private Function<CompanyPhone, String> makeCompanyPhone = 
			p -> p!=null?p.getPhone():null;

			
	public Function<Company, CompanyDTO> makeNewCompanyDTO = 
			company -> new CompanyDTO(company.getCompany(),
										makeNewCompanyAddressDTO.apply(company.getCompanyAddress()),
										makeCompanyPhone.apply(company.getCompanyPhone()));
			
	private Function<UserCompany, CompanyDTO> makeNewCompanyDTOByUserCompany =
			userCompany -> makeNewCompanyDTO.apply(userCompany.getCompany());

	private Predicate<User> isPrimaryContact =
				u -> u.getUserCompany()!=null?u.getUserCompany().isPrimary():false;

	private Predicate<User> isAdministrativeContact =
			u -> u.getUserCompany()!=null?u.getUserCompany().isAdministrative():false;
	
	private Predicate<User> isBillingContact =
			u -> u.getUserCompany()!=null?u.getUserCompany().isBilling():false;

	public Function<User, AccountDTO> makeAccountDTO = 
			user -> new AccountDTO(user.getUserId(),
									user.getUsername(),
									user.getEmail(),
									user.getFirstName(),
									user.getMiddleInitial(),
									user.getLastName(),
									makeNewAddressDTOByType.apply(SHIPPING_ADDRESS).apply(user.getAddresses()),
									makeNewAddressDTOByType.apply(MAILING_ADDRESS).apply(user.getAddresses()),
									makeNewPhoneDTOByType.apply(MOBILE_PHONE).apply(user.getPhones()),
									makeNewPhoneDTOByType.apply(HOME_PHONE).apply(user.getPhones()),
									makeNewPhoneDTOByType.apply(WORK_PHONE).apply(user.getPhones()),
									user.getUserCompany()!=null?makeNewCompanyDTOByUserCompany.apply(user.getUserCompany()):null,
									isPrimaryContact.test(user),
									isAdministrativeContact.test(user),
									isBillingContact.test(user),
									makeNewRoleDTOs.apply(user.getUserRoles()));
										
	public Function<Collection<User>, Collection<AccountDTO>> makeAccountDTOs =
			users -> users.parallelStream()
						.map(u -> makeAccountDTO.apply(u))
						.collect(Collectors.toList());
			
	public Function<Collection<AddressType> , Collection<String>> makeNewAddressTypesDTO =
			at -> at
					.stream()
					.map(t -> new String(t.getType()))
					.collect(Collectors.toList());
			
			
	public Collection<AccountDTO> getAccounts() {
		Session session = createSession();
		try {
			return makeAccountDTOs.apply(DataManager.getUsers(session));
		} catch (Exception e) {
			throw logAndThrowError("Error getting accounts.", e);
		} finally {
			endSession(session);
		}
	}
	
	
	public Collection<String> getAddressTypes() {
		Session session = createSession();
		try {
			return makeNewAddressTypesDTO.apply(DataManager.getAddressTypes(session));
		} catch (Exception e) {
			throw logAndThrowError("Error getting address types.", e);
		} finally {
			endSession(session);
		}
	} 
			
			
}
