package com.akmade.security.data;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.Session;

import com.akmade.security.data.Address;
import com.akmade.security.data.AddressType;
import com.akmade.security.data.Company;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.CompanyPhone;
import com.akmade.security.data.Phone;
import com.akmade.security.data.PhoneType;
import com.akmade.security.data.Role;
import com.akmade.security.data.RoleType;
import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
import com.akmade.security.data.UserRole;

public class DataOutputManager extends DataSession {
	
	private static Function<Object, Consumer<Session>> delete =
				o ->
					s -> s.delete(o);
				
	private static Function<Object, Consumer<Session>> save =
				o ->
					s -> s.saveOrUpdate(o);

	
	private static <T> Consumer<Session> prepareTransaction(Function<Object, Consumer<Session>> txn, Object o) {
		return o!=null?txn.apply(o):s -> {};
	}
	
	private static <T> Consumer<Session> prepareTransaction(Function<Object, Consumer<Session>> txn, Collection<T> objects) {
		return prepareTransaction(txn, objects.stream());
	}
	
	private static <T> Consumer<Session> prepareTransaction(Function<Object, Consumer<Session>> txn, Stream<T> stream) {
		return stream
				.map(o -> txn.apply(o))
				.reduce(Consumer::andThen)
				.orElse(t -> {});
	}

	public static Consumer<Session> saveAddressTypes(Collection<AddressType> addressTypes) {
		logger.info("Saving " + addressTypes.size() + " address types.");
		return prepareTransaction(save, addressTypes);
	}
	
	public static Consumer<Session> saveAddressType(AddressType addressType) {
		logger.info("Saving " + addressType.getType() + ".");
		return prepareTransaction(save, addressType);
	}
	
	public static Consumer<Session> deleteAddressTypes(Collection<AddressType> addressTypes) {
		logger.info("Deleting " + addressTypes.size() + " address types.");
		return prepareTransaction(delete, addressTypes);
	}
	
	public static Consumer<Session> deleteAddressType(AddressType addressType) {
		logger.info("Deleting " + addressType.getType() + ".");
		return prepareTransaction(delete, addressType);
	}
	
/*
	
	public void deletePhoneTypes(Collection<PhoneType> phoneTypes) {
		logger.info("Deleting " + phoneTypes.size() + " phone types.");
		runTransaction(prepareTransaction(delete, phoneTypes));
	}
	
	public void deletePhoneType(PhoneType phoneType) {
		logger.info("Deleting " + phoneType.getType() + ".");
		runTransaction(prepareTransaction(delete, phoneType));
	}
	
	public void savePhoneTypes(Collection<PhoneType> phoneTypes) {
		logger.info("Saving " + phoneTypes.size() + " phone types.");
		runTransaction(prepareTransaction(save, phoneTypes));
	}
	
	public void savePhoneType(PhoneType phoneType) {
		logger.info("Saving " + phoneType.getType() + ".");
		runTransaction(prepareTransaction(save, phoneType));
	}
	

	
	
	public void deleteRoleTypes(Collection<RoleType> roleTypes) {
		logger.info("Deleting " + roleTypes.size() + " role types.");
		runTransaction(prepareTransaction(delete, roleTypes));
	}
	
	public void deleteRoleType(RoleType roleType) {
		logger.info("Deleting " + roleType.getType() + ".");
		runTransaction(prepareTransaction(delete, roleType));
	}
	
	public void saveRoleTypes(Collection<RoleType> roleTypes) {
		logger.info("Saving " + roleTypes.size() + " role types.");
		runTransaction(prepareTransaction(save, roleTypes));
	}
	
	public void saveRoleType(RoleType roleType) {
		logger.info("Saving " + roleType.getType() + ".");
		runTransaction(prepareTransaction (save, roleType));
	}
	
	public void deleteAddresses(Collection<Address> addresses) {
		logger.info("Deleting " + addresses.size() + " addresses.");
		runTransaction(prepareTransaction(delete, addresses));
	}
	
	public void deleteAddress(Address address) {
		logger.info("Deleting address.");
		runTransaction(prepareTransaction(delete, address));
	}
	
	public void saveAddresses(Collection<Address> addresses) {
		logger.info("Saving " + addresses.size() + " addresses.");
		runTransaction(prepareTransaction(save, addresses));
	}
	
	public void saveAddress(Address address) {
		logger.info("Saving address.");
		runTransaction(prepareTransaction(save, address));
	}
	
	
	public void deleteCompanies(Collection<Company> companies) {
		logger.info("Deleting " + companies.size() + " companies.");
		
		runTransaction(
				prepareTransaction(delete, companies.stream()
												.map(c -> c.getCompanyAddress()))
				.andThen(prepareTransaction(delete, companies.stream()
															.map(c -> c.getCompanyPhone())))
				.andThen(prepareTransaction(delete, companies.stream()
															.flatMap(c -> c.getUserCompanies().stream())))
				.andThen(prepareTransaction(delete, companies)));
	}
	
	public void deleteCompany(Company company) {
		logger.info("Deleting " + company.getCompany() + ".");
		
		runTransaction(
				prepareTransaction(delete,company.getCompanyAddress())
					.andThen(prepareTransaction(delete, company.getCompanyPhone()))
					.andThen(prepareTransaction(delete, company.getUserCompanies()))
					.andThen(prepareTransaction(delete,company)));
	}
	
	
	public void saveCompanies(Collection<Company> companies) {
		logger.info("Saving " + companies.size() + " companies.");
		
		runTransaction(
					prepareTransaction(save, companies)
						.andThen(prepareTransaction(save, companies.stream()
																.map(c -> c.getCompanyAddress())))
						.andThen(prepareTransaction(save, companies.stream()
																.map(c -> c.getCompanyPhone())))
						.andThen(prepareTransaction(save, companies.stream()
																.filter(c -> c.getUserCompanies() != null)
																.flatMap(c -> c.getUserCompanies().stream()))));
	}
	
	public void saveCompany(Company company) {
		logger.info("Saving " + company.getCompany() + ".");
		
		runTransaction(save.apply(company)
				.andThen(prepareTransaction(save, company.getCompanyAddress()))
				.andThen(prepareTransaction(save, company.getCompanyPhone()))
				.andThen(prepareTransaction(save, company.getUserCompanies())));
	}

	public void deleteCompanyAddresses(Collection<CompanyAddress> addresses) {
		logger.info("Deleting " + addresses.size() + " company addresses.");
		runTransaction(prepareTransaction(delete, addresses));
	}
	
	public void deleteCompanyAddress(CompanyAddress address) {
		logger.info("Deleting company address.");
		runTransaction(prepareTransaction(delete, address));
	}
	
	public void saveCompanyAddresses(Collection<CompanyAddress> addresses) {
		logger.info("Saving " + addresses.size() + " company addresses.");
		runTransaction(prepareTransaction(save, addresses));
	}
	
	public void saveCompanyAddress(CompanyAddress address) {
		logger.info("Saving company address.");
		runTransaction(prepareTransaction(save, address));
	}
	
	public void deleteCompanyPhones(Collection<CompanyPhone> phones) {
		logger.info("Deleting " + phones.size() + " company phones.");
		runTransaction(prepareTransaction(delete, phones));
	}
	
	public void deleteCompanyPhone(CompanyPhone phone) {
		logger.info("Deleting company phone.");
		runTransaction(prepareTransaction(delete, phone));
	}
	
	public void saveCompanyPhones(Collection<CompanyPhone> phones) {
		logger.info("Saving " + phones.size() + " company phones.");
		runTransaction(prepareTransaction(save, phones));
	}
	
	public void saveCompanyPhone(CompanyPhone phone) {
		logger.info("Saving company phone.");
		runTransaction(prepareTransaction(save, phone));
	}

	public void deletePhones(Collection<Phone> phones) {
		logger.info("Deleting " + phones.size() + "  phones.");
		runTransaction(prepareTransaction(delete, phones));
	}
	
	public void deletePhone(Phone phone) {
		logger.info("Deleting phone.");
		runTransaction(prepareTransaction(delete, phone));
	}
	
	public void savePhones(Collection<Phone> phones) {
		logger.info("Saving " + phones.size() + " phones.");
		runTransaction(prepareTransaction(save, phones));
	}
	
	public void savePhone(Phone phone) {
		logger.info("Saving phone.");
		runTransaction(prepareTransaction(save, phone));
	}

	
	public void deleteRoles(Collection<Role> roles) {
		logger.info("Deleting " + roles.size() + "  roles.");
		runTransaction(prepareTransaction(delete, roles.stream()
														.flatMap(r -> r.getUserRoles().stream()))
							.andThen(prepareTransaction(delete, roles)));
	}
	
	public void deleteRole(Role role) {
		logger.info("Deleting role.");
		runTransaction(prepareTransaction(delete, role.getUserRoles())
						.andThen(prepareTransaction(delete, role)));
	}
	
	public void saveRoles(Collection<Role> roles) {
		logger.info("Saving " + roles.size() + " roles.");
		runTransaction(prepareTransaction(save, roles.stream()
													.flatMap(r -> r.getUserRoles().stream()))
						.andThen(prepareTransaction(save, roles)));
	}
	
	public void saveRole(Role role) {
		logger.info("Saving role.");
		runTransaction(
				prepareTransaction(save, role.getUserRoles())
				.andThen(prepareTransaction(save, role)));
	}
	
	
	public void deleteUserCompanies(Collection<UserCompany> userCompanies) {
		logger.info("Deleting " + userCompanies.size() + " user companies.");
		runTransaction(prepareTransaction(delete, userCompanies));
	}
	
	public void deleteUserCompany(UserCompany userCompany) {
		logger.info("Deleting user company.");
		runTransaction(prepareTransaction(delete, userCompany));
	}
	
	public void saveUserCompanies(Collection<UserCompany> userCompanies) {
		logger.info("Saving " + userCompanies.size() + " user companies.");
		runTransaction(prepareTransaction(save, userCompanies));
	}
	
	public void saveUserCompany(UserCompany userCompany) {
		logger.info("Saving user company.");
		runTransaction(prepareTransaction(save, userCompany));
	}

	
	public void deleteUserRoles(Collection<UserRole> userRoles) {
		logger.info("Deleting " + userRoles.size() + " user roles.");
		runTransaction(prepareTransaction(delete, userRoles));
	}
	
	public void deleteUserRole(UserRole userRole) {
		logger.info("Deleting user role.");
		runTransaction(prepareTransaction(delete, userRole));
	}
	
	public void saveUserRoles(Collection<UserRole> userRoles) {
		logger.info("Saving " + userRoles.size() + " user roles.");
		runTransaction(prepareTransaction(save, userRoles));
	}
	
	public void saveUserRole(UserRole userRole) {
		logger.info("Saving user role.");
		runTransaction(prepareTransaction(save, userRole));
	}

	
	public void deleteUsers(Collection<User> users) {
		logger.info("Deleting " + users.size() + " users.");
		
		runTransaction(prepareTransaction(delete, users.stream()
														.filter(u -> u.getAddresses() != null)
														.flatMap(u -> u.getAddresses().stream()))
				.andThen(prepareTransaction(delete, users.stream()
														.filter(u -> u.getPhones() != null)
														.flatMap(u -> u.getPhones().stream())))
				.andThen(prepareTransaction(delete, users.stream()
														.filter(u -> u.getUserRoles() != null)
														.flatMap(u -> u.getUserRoles().stream())))
				.andThen(prepareTransaction(delete, users.stream()
														.filter(u -> u.getUserCompany() != null)
														.map(u -> u.getUserCompany())))
				.andThen(prepareTransaction(delete, users)));
	}
	
	public void deleteUser(User user) {
		logger.info("Deleting " + user.getUsername() + " user.");
		
		runTransaction(
			prepareTransaction(delete, user.getAddresses())
				.andThen(prepareTransaction(delete, user.getPhones()))
				.andThen(prepareTransaction(delete, user.getUserRoles()))
				.andThen(prepareTransaction(delete, user.getUserCompany()))
				.andThen(prepareTransaction(delete, user)));
	}

	public void saveUsers(Collection<User> users) {
		logger.info("Saving " + users.size() + " users.");
		
		runTransaction(prepareTransaction(save, users)
				.andThen(prepareTransaction(save, users.stream()
														.filter(u -> u.getAddresses() != null)
														.flatMap(u -> u.getAddresses().stream())))
				.andThen(prepareTransaction(save, users.stream()
														.filter(u -> u.getPhones() != null)
														.flatMap(u -> u.getPhones().stream())))
				.andThen(prepareTransaction(save, users.stream()
														.filter(u -> u.getUserRoles() != null)
														.flatMap(u -> u.getUserRoles().stream())))
				.andThen(prepareTransaction(save, users.stream()
														.filter(u -> u.getUserCompany() != null)
														.map(u -> u.getUserCompany()))));
	}

	public void saveUser(User user) {
		logger.info("Saving " + user.getUsername() + " user.");
		
		runTransaction(save.apply(user)
				.andThen(prepareTransaction(save, user.getAddresses()))
				.andThen(prepareTransaction(save, user.getPhones()))
				.andThen(prepareTransaction(save, user.getUserRoles()))
				.andThen(prepareTransaction(save, user.getUserCompany()))
				.andThen(prepareTransaction(save, user)));
	}*/
}
