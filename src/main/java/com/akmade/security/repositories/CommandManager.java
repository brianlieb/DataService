package com.akmade.security.repositories;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hibernate.Session;

import com.akmade.security.data.Address;
import com.akmade.security.data.AddressType;
import com.akmade.security.data.Company;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.CompanyPhone;
import com.akmade.security.data.Phone;
import com.akmade.security.data.PhoneType;
import com.akmade.security.data.RoleType;
import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
import com.akmade.security.data.UserRole;
import com.akmade.security.repositories.SessionRepo.Txn;

public class CommandManager extends DataSession {
	protected static Txn doNothing =
				session -> {};
			
			
	private static Function<Session, Predicate<Object>> hasId =
			s -> 
				o -> {
					if (s.getIdentifier(o)!=null)
						return true;
					else
						return false;
				};
	
	private static Function<Object, Txn> delete =
				o ->
					s -> 
					{	
						if (hasId.apply(s).test(o))
							s.delete(o);
					};
				
	private static Function<Object, Txn> save =
				o ->
					s -> s.saveOrUpdate(o);
	
	private static Function<Function<Object, Txn>, Function<Object, Txn>> prepareTransaction =
			fn ->
				o -> o!=null?fn.apply(o):doNothing;
					
	private static <T> Txn prepareTransaction(Function<Object, Txn> txn, Collection<T> objects) {
		return prepareTransaction(txn, objects.stream());
	}

	private static <T> Txn prepareTransaction(Function<Object, Txn> txn, Stream<T> stream) {
		return stream
				.map(o -> prepareTransaction.apply(txn).apply(o))
				.reduce((t1, t2) -> t1.andThen(t2))
				.orElse(doNothing);
	}

	protected static Function<AddressType, Txn> saveAddressType =
		addressType -> prepareTransaction.apply(save).apply(addressType);
	
	protected static Function<Collection<AddressType>, Txn> saveAddressTypes =
		addressTypes ->  prepareTransaction(save, addressTypes);

	protected static Function<AddressType, Txn> deleteAddressType =
		addressType -> prepareTransaction.apply(delete).apply(addressType);

	protected static Function<Collection<AddressType>, Txn> deleteAddressTypes =
			addressTypes ->  prepareTransaction(delete, addressTypes);


			
	protected static Function<RoleType, Txn> saveRoleType =
			roleType -> prepareTransaction.apply(save).apply(roleType);

	protected static Function<Collection<RoleType>, Txn> saveRoleTypes =
			roleTypes ->  prepareTransaction(save, roleTypes);
			
	protected static Function<RoleType, Txn> deleteRoleType =
			roleType -> prepareTransaction.apply(delete).apply(roleType);

	protected static Function<Collection<RoleType>, Txn> deleteRoleTypes =
			roleTypes ->  prepareTransaction(delete, roleTypes);

	
	protected static Function<PhoneType, Txn> savePhoneType =
			phoneType -> prepareTransaction.apply(save).apply(phoneType);

	protected static Function<Collection<PhoneType>, Txn> savePhoneTypes =
			phoneTypes ->  prepareTransaction(save, phoneTypes);
			
	protected static Function<PhoneType, Txn> deletePhoneType =
			phoneType -> prepareTransaction.apply(delete).apply(phoneType);

	protected static Function<Collection<PhoneType>, Txn> deletePhoneTypes =
			phoneTypes ->  prepareTransaction(delete, phoneTypes);
			

	
	protected static Function<CompanyAddress, Txn> deleteCompanyAddress =
			companyAddress -> prepareTransaction.apply(delete).apply(companyAddress);
							
	protected static Function<CompanyPhone, Txn> deleteCompanyPhone =
			companyPhone -> prepareTransaction.apply(delete).apply(companyPhone);
							
	protected static Function<UserCompany, Txn> deleteUserCompany = 
			userCompany ->	prepareTransaction.apply(delete).apply(userCompany);
			
	protected static Function<Collection<UserCompany>, Txn> deleteUserCompanies = 
			userCompanies -> prepareTransaction(delete, userCompanies);
	
	protected static Function<Company, Txn> deleteCompanyTree = 
			company ->	deleteCompanyAddress.apply(company.getCompanyAddress())
											.andThen(deleteCompanyPhone.apply(company.getCompanyPhone()))
											.andThen(deleteUserCompanies.apply(company.getUserCompanies()))
											.andThen(prepareTransaction.apply(delete).apply(company));
			
	protected static Function<Collection<Company>, Txn> deleteCompanyTrees =
			companies -> companies
							.stream()
							.map(c -> deleteCompanyTree.apply(c))
							.reduce(Txn::andThen)
							.orElse(doNothing);

	
	protected static Function<CompanyAddress, Txn> saveCompanyAddress =
			companyAddress -> prepareTransaction.apply(save).apply(companyAddress);
									
	protected static Function<CompanyPhone, Txn> saveCompanyPhone =
			companyPhone -> prepareTransaction.apply(save).apply(companyPhone);
			
	protected static Function<UserCompany, Txn> saveUserCompany = 
			companyUser ->	prepareTransaction.apply(save).apply(companyUser);
										
	protected static Function<Collection<UserCompany>, Txn> saveUserCompanies = 
			companyUsers ->	prepareTransaction(save, companyUsers);

	protected static Function<Company, Txn> saveCompany =
			company -> prepareTransaction.apply(save).apply(company);

	protected static Function<Company, Txn> saveCompanyTree = 
			company ->  saveCompany.apply(company)
							.andThen(saveCompanyAddress.apply(company.getCompanyAddress()))
							.andThen(saveCompanyPhone.apply(company.getCompanyPhone()))
							.andThen(saveUserCompanies.apply(company.getUserCompanies()));
			
			
	protected static Function<Address, Txn> deleteAddress =
			address -> prepareTransaction.apply(delete).apply(address);

	protected static Function<Collection<Address>, Txn> deleteAddresses =
			addresses -> prepareTransaction(delete, addresses);
				
	protected static Function<Address, Txn> saveAddress =
			address -> prepareTransaction.apply(save).apply(address);

	protected static Function<Collection<Address>, Txn> saveAddresses =
			addresses -> prepareTransaction(save, addresses);
			
			
	protected static Function<Phone, Txn> deletePhone =
			phone -> prepareTransaction.apply(delete).apply(phone);

	protected static Function<Collection<Phone>, Txn> deletePhones =
			phones -> prepareTransaction(delete, phones);
				
	protected static Function<Phone, Txn> savePhone =
			phone -> prepareTransaction.apply(save).apply(phone);

	protected static Function<Collection<Phone>, Txn> savePhones =
			phones -> prepareTransaction(save, phones);
			
			
	
	protected static Function<UserRole, Txn> deleteUserRole =
			userRole -> prepareTransaction.apply(delete).apply(userRole);

	protected static Function<Collection<UserRole>, Txn> deleteUserRoles =
			userRoles -> prepareTransaction(delete, userRoles);
				
	protected static Function<UserRole, Txn> saveUserRole =
			userRole -> prepareTransaction.apply(save).apply(userRole);

	protected static Function<Collection<UserRole>, Txn> saveUserRoles =
			userRoles -> prepareTransaction(save, userRoles);
	
	protected static Function<User, Txn> saveUser =
			user -> prepareTransaction.apply(save).apply(user);
			

	protected static Function<User, Txn> saveUserTree =
			user ->  saveUser.apply(user)
							.andThen(savePhones.apply(user.getPhones()))
							.andThen(saveAddresses.apply(user.getAddresses()))
							.andThen(saveUserRoles.apply(user.getUserRoles()))
							.andThen(saveUserCompany.apply(user.getUserCompany()));
			
	protected static Function<Collection<User>, Txn> saveUserTrees =
			users -> users.stream()
						.map(u -> saveUserTree.apply(u))
						.reduce(Txn::andThen)
						.orElse(doNothing);
			

	protected static Function<User, Txn> deleteUser =
			user ->		deleteUserRoles.apply(user.getUserRoles())
							.andThen(deleteAddresses.apply(user.getAddresses()))
							.andThen(deletePhones.apply(user.getPhones()))
							.andThen(deleteUserCompany.apply(user.getUserCompany()))
							.andThen(prepareTransaction.apply(delete).apply(user));
	
	protected static Function<Collection<User>, Txn> deleteUsers =
			users -> users.stream()
						.map(u -> deleteUser.apply(u))
						.reduce(Txn::andThen)
						.orElse(doNothing);
	
}
