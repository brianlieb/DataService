package com.akmade.security.repositories;

import java.util.Collection;
import java.util.function.Consumer;
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

public class CommandManager extends DataSession {
	protected static Consumer<Session> doNothing =
				session -> {};
			
			
	private static Function<Session, Predicate<Object>> hasId =
			s -> 
				o -> {
					if (s.getIdentifier(o)!=null)
						return true;
					else
						return false;
				};
	
	private static Function<Object, Consumer<Session>> delete =
				o ->
					s -> 
					{	
						if (hasId.apply(s).test(o))
							s.delete(o);
					};
				
	private static Function<Object, Consumer<Session>> save =
				o ->
					s -> s.saveOrUpdate(o);
	
	private static Function<Function<Object, Consumer<Session>>, Function<Object, Consumer<Session>>> prepareTransaction =
			fn ->
				o -> o!=null?fn.apply(o):doNothing;
					
	private static <T> Consumer<Session> prepareTransaction(Function<Object, Consumer<Session>> txn, Collection<T> objects) {
		return prepareTransaction(txn, objects.stream());
	}

	private static <T> Consumer<Session> prepareTransaction(Function<Object, Consumer<Session>> txn, Stream<T> stream) {
		return stream
				.map(o -> prepareTransaction.apply(txn).apply(o))
				.reduce((t1, t2) -> t1.andThen(t2))
				.orElse(doNothing);
	}

	protected static Function<AddressType, Consumer<Session>> saveAddressType =
		addressType -> prepareTransaction.apply(save).apply(addressType);
	
	protected static Function<Collection<AddressType>, Consumer<Session>> saveAddressTypes =
		addressTypes ->  prepareTransaction(save, addressTypes);

	protected static Function<AddressType, Consumer<Session>> deleteAddressType =
		addressType -> prepareTransaction.apply(delete).apply(addressType);

	protected static Function<Collection<AddressType>, Consumer<Session>> deleteAddressTypes =
			addressTypes ->  prepareTransaction(delete, addressTypes);


			
	protected static Function<RoleType, Consumer<Session>> saveRoleType =
			roleType -> prepareTransaction.apply(save).apply(roleType);

	protected static Function<Collection<RoleType>, Consumer<Session>> saveRoleTypes =
			roleTypes ->  prepareTransaction(save, roleTypes);
			
	protected static Function<RoleType, Consumer<Session>> deleteRoleType =
			roleType -> prepareTransaction.apply(delete).apply(roleType);

	protected static Function<Collection<RoleType>, Consumer<Session>> deleteRoleTypes =
			roleTypes ->  prepareTransaction(delete, roleTypes);

	
	protected static Function<PhoneType, Consumer<Session>> savePhoneType =
			phoneType -> prepareTransaction.apply(save).apply(phoneType);

	protected static Function<Collection<PhoneType>, Consumer<Session>> savePhoneTypes =
			phoneTypes ->  prepareTransaction(save, phoneTypes);
			
	protected static Function<PhoneType, Consumer<Session>> deletePhoneType =
			phoneType -> prepareTransaction.apply(delete).apply(phoneType);

	protected static Function<Collection<PhoneType>, Consumer<Session>> deletePhoneTypes =
			phoneTypes ->  prepareTransaction(delete, phoneTypes);
			

	
	protected static Function<CompanyAddress, Consumer<Session>> deleteCompanyAddress =
			companyAddress -> prepareTransaction.apply(delete).apply(companyAddress);
							
	protected static Function<CompanyPhone, Consumer<Session>> deleteCompanyPhone =
			companyPhone -> prepareTransaction.apply(delete).apply(companyPhone);
							
	protected static Function<UserCompany, Consumer<Session>> deleteUserCompany = 
			userCompany ->	prepareTransaction.apply(delete).apply(userCompany);
			
	protected static Function<Collection<UserCompany>, Consumer<Session>> deleteUserCompanies = 
			userCompanies -> prepareTransaction(delete, userCompanies);
	
	protected static Function<Company, Consumer<Session>> deleteCompanyTree = 
			company ->	deleteCompanyAddress.apply(company.getCompanyAddress())
											.andThen(deleteCompanyPhone.apply(company.getCompanyPhone()))
											.andThen(deleteUserCompanies.apply(company.getUserCompanies()))
											.andThen(prepareTransaction.apply(delete).apply(company));
			
	protected static Function<Collection<Company>, Consumer<Session>> deleteCompanyTrees =
			companies -> companies
							.stream()
							.map(c -> deleteCompanyTree.apply(c))
							.reduce(Consumer::andThen)
							.orElse(doNothing);

	
	protected static Function<CompanyAddress, Consumer<Session>> saveCompanyAddress =
			companyAddress -> prepareTransaction.apply(save).apply(companyAddress);
									
	protected static Function<CompanyPhone, Consumer<Session>> saveCompanyPhone =
			companyPhone -> prepareTransaction.apply(save).apply(companyPhone);
			
	protected static Function<UserCompany, Consumer<Session>> saveUserCompany = 
			companyUser ->	prepareTransaction.apply(save).apply(companyUser);
										
	protected static Function<Collection<UserCompany>, Consumer<Session>> saveUserCompanies = 
			companyUsers ->	prepareTransaction(save, companyUsers);

	protected static Function<Company, Consumer<Session>> saveCompany =
			company -> prepareTransaction.apply(save).apply(company);

	protected static Function<Company, Consumer<Session>> saveCompanyTree = 
			company ->  saveCompany.apply(company)
							.andThen(saveCompanyAddress.apply(company.getCompanyAddress()))
							.andThen(saveCompanyPhone.apply(company.getCompanyPhone()))
							.andThen(saveUserCompanies.apply(company.getUserCompanies()));
			
			
	protected static Function<Address, Consumer<Session>> deleteAddress =
			address -> prepareTransaction.apply(delete).apply(address);

	protected static Function<Collection<Address>, Consumer<Session>> deleteAddresses =
			addresses -> prepareTransaction(delete, addresses);
				
	protected static Function<Address, Consumer<Session>> saveAddress =
			address -> prepareTransaction.apply(save).apply(address);

	protected static Function<Collection<Address>, Consumer<Session>> saveAddresses =
			addresses -> prepareTransaction(save, addresses);
			
			
	protected static Function<Phone, Consumer<Session>> deletePhone =
			phone -> prepareTransaction.apply(delete).apply(phone);

	protected static Function<Collection<Phone>, Consumer<Session>> deletePhones =
			phones -> prepareTransaction(delete, phones);
				
	protected static Function<Phone, Consumer<Session>> savePhone =
			phone -> prepareTransaction.apply(save).apply(phone);

	protected static Function<Collection<Phone>, Consumer<Session>> savePhones =
			phones -> prepareTransaction(save, phones);
			
			
	
	protected static Function<UserRole, Consumer<Session>> deleteUserRole =
			userRole -> prepareTransaction.apply(delete).apply(userRole);

	protected static Function<Collection<UserRole>, Consumer<Session>> deleteUserRoles =
			userRoles -> prepareTransaction(delete, userRoles);
				
	protected static Function<UserRole, Consumer<Session>> saveUserRole =
			userRole -> prepareTransaction.apply(save).apply(userRole);

	protected static Function<Collection<UserRole>, Consumer<Session>> saveUserRoles =
			userRoles -> prepareTransaction(save, userRoles);
	
	protected static Function<User, Consumer<Session>> saveUser =
			user -> prepareTransaction.apply(save).apply(user);
			

	protected static Function<User, Consumer<Session>> saveUserTree =
			user ->  saveUser.apply(user)
							.andThen(savePhones.apply(user.getPhones()))
							.andThen(saveAddresses.apply(user.getAddresses()))
							.andThen(saveUserRoles.apply(user.getUserRoles()))
							.andThen(saveUserCompany.apply(user.getUserCompany()));
			
	protected static Function<Collection<User>, Consumer<Session>> saveUserTrees =
			users -> users.stream()
						.map(u -> saveUserTree.apply(u))
						.reduce(Consumer::andThen)
						.orElse(doNothing);
			

	protected static Function<User, Consumer<Session>> deleteUser =
			user ->		deleteUserRoles.apply(user.getUserRoles())
							.andThen(deleteAddresses.apply(user.getAddresses()))
							.andThen(deletePhones.apply(user.getPhones()))
							.andThen(deleteUserCompany.apply(user.getUserCompany()))
							.andThen(prepareTransaction.apply(delete).apply(user));
	
	protected static Function<Collection<User>, Consumer<Session>> deleteUsers =
			users -> users.stream()
						.map(u -> deleteUser.apply(u))
						.reduce(Consumer::andThen)
						.orElse(doNothing);
	
}
