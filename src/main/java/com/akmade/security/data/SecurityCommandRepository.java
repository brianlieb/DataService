package com.akmade.security.data;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.Session;

public class SecurityCommandRepository extends SecuritySessionRepository {
	
	private AtomicInteger addressTypeId = null;
	
	public SecurityCommandRepository() {
	}


	
	private Function<ImmutablePair<String, String>, Function<Session, AddressType>> makeAddressTypeFromOld =
			pair ->
				session -> {
					AddressType addressType = DataManager.getAddressTypeByType(pair.left,session);
					if (addressType != null) {
						return makeNewAddressType(addressType.getAddressTypeId(), pair.left, pair.right, addressType.getAddresses());
					} else {
						return null;
					}
				};
			
	private Function<ImmutablePair<String, String>, Function<Session, AddressType>> makeAddressTypeFromNew =
			pair ->
				session -> makeNewAddressType(DataManager.getAddressTypeMaxId(session)+1, pair.left, pair.right, null);
				
	private AddressType makeNewAddressType(int id, String type, String description, Set<Address> addresses) {
		return new AddressType(id, type, description, addresses);
	}
	
	
	public void saveAddressType(ImmutablePair<String, String> dto) {
		logger.info("Making Address Type for: " + dto.left + " " + dto.right);
 		AddressType addressType = (AddressType) makeHibernate(makeAddressTypeFromNew.apply(dto), "Address Type");
		runTransaction(DataOutputManager.saveAddressType(addressType));
	}
	
	private <T> Object makeHibernate(Function<Session, T> fn, String objectType) {
		Session session = createSession(dataSource);
		logger.info("Making hibernate object " + objectType);
		try {
			return fn.apply(session);
		} catch (Exception e) {
			rollback(session);
			throw logAndThrowError("Error creating the " + objectType +"." + e.getMessage());
		} finally {
			logger.info("made");
			endSession(session);
		}
	}
	
	private void runTransaction(Consumer<Session> c) {
		Session session = createSession(dataSource);
		logger.debug("Saving!");
		try {
			c.accept(session);
		} catch (Exception e) {
			rollbackAndClose(session);
			throw logAndThrowError("Error running the transaction. " + e.getMessage());
		} finally {
			logger.debug("saved");
			endSession(session);
		}			
	}
			
			
/*			
	private static final Function<String, RoleType> findRoleType = 
			type -> dataMan.getRoleTypes()
							.stream()
							.filter(t -> t.getType().equals(type))
							.findFirst()
							.get();
			
			
	private static final Function<String, PhoneType> findPhoneType = 
			type -> dataMan.getPhoneTypes()
							.stream()
							.filter(t -> t.getType().equals(type))
							.findFirst()
							.get();
	
	private static final Function<AccountDTO, Collection<Phone>> findPhones =
		acct -> dataMan.getPhonesByUser(acct.userName);

	private static final Function<AccountDTO, Collection<Address>> findAddresses =
			acct -> dataMan.getAddressesByUser(acct.userName);
			
	

	private static final BiFunction<AddressDTO, Address, Address> makeExistingAddress =
			(dto, address) -> { Address newAddress = 
										new Address(address.getAddressType(),
													address.getUser(),
													dto.address1!=null?dto.address1:address.getAddress1(),
													dto.address2!=null?dto.address2:address.getAddress2(),
													dto.city!=null?dto.city:address.getCity(),
													dto.state!=null?dto.state:address.getState(),
													dto.country!=null?dto.country:address.getCountry(),
													dto.postalCode!=null?dto.postalCode:address.getPostalCode(),
													address.getCreatedDate()!=null?address.getCreatedDate():new Date(),
													new Date());
	
										if (address.getAddressId()!=null)
											newAddress.setAddressId(address.getAddressId());
										
										return newAddress;
							};		
							
							
	public User findAddresses(AccountDTO dto) {
		User user = dataMan.getUserByUsername(dto.userName);
		
		if (user == null) {
			makeUserFromDTO(dto);
		} else {
			makeUserFromDTOAccount(dto, user);
		}
	}
	
	
	
	private static User makeUserFromDTO(AccountDTO dto) {
		User newUser = new User();
		Collection<Address> addresses = makeAddressesFromDTO(dto, newUser);
		Collection<Phone> phones = makePhonesFromDTO(dto, newUser);
		
		
		User newUser = makeUser.apply(dto);
		);
		newUser.getUserCompanies().addAll(makeUserCompanies(newUser, makeCompanyFromDTO(dto.company, newUser), dto.billing, dto.administrative, dto.billing));
		newUser.getUserRoles().addAll(makeUserRoles(newUser, makeRolesFromDTO(dto.roles)));
		return newUser;
	}
	
	
	
	private static final Function<AccountDTO, User> makeUser =
			account -> {User user = new User(account.userName,
												null, // TODO password
												account.email,
												account.firstName,
												account.middleInitial,
												account.lastName,
												false,
												new Date(),
												new Date(),
												null, //TODO addresses
												null, //TODO phones
												null, //TODO usercompanies,
												null); //TODO userRoles
			
						return user;
			};
							
	
			
			
	
	

	
	
	private static Collection<UserCompany> makeUserCompanies(User user, Company company, Boolean primary, Boolean administrative, Boolean billing) {
		Collection<UserCompany> userCompanies = new HashSet<>();
		userCompanies.add(new UserCompany(new UserCompanyId(),
											company,
											user,
											primary,
											administrative,
											billing));
		
		return userCompanies;
	}
	
	private static Collection<UserRole> makeUserRoles(User user, Collection<Role> roles ) {
		return roles.stream()
				.map(r -> new UserRole(0, r, user))
				.collect(Collectors.toSet());
	}
	
	
	private static Collection<Role> makeRolesFromDTO(Collection<RoleDTO> roles) {
		return roles.stream()
				.map(r -> new Role(findRoleType.apply(r.roleType), r.role, r.description, null))
				.collect(Collectors.toSet());
	}
	
			
	private static Collection<Address> makeAddressesFromDTO(AccountDTO dto, User user) {
		Collection<Address> addresses = new HashSet<Address>();
		
		addresses.add(new Address(findAddressType.apply(SHIPPING_ADDRESS),
									user,
									dto.shippingAddress.address1,
									dto.shippingAddress.address2,
									dto.shippingAddress.city,
									dto.shippingAddress.state,
									dto.shippingAddress.country,
									dto.shippingAddress.postalCode,
									new Date(),
									new Date()));

		addresses.add(new Address(findAddressType.apply(MAILING_ADDRESS),
									user,
									dto.mailingAddress.address1,
									dto.mailingAddress.address2,
									dto.mailingAddress.city,
									dto.mailingAddress.state,
									dto.mailingAddress.country,
									dto.mailingAddress.postalCode,
									new Date(),
									new Date()));
		
		return addresses;
	}
	
	private static Function<User, BiFunction<String, PhoneType, Phone>> makeNewPhone =
		user ->
			(phone, type) -> new Phone(type,
										user,
										phone,
										new Date(),
										new Date());
	
	private static Collection<Phone> makePhonesFromDTO(AccountDTO dto, User user) {
		Collection<Phone> phones = new HashSet<>();
		
		if (dto.mobilePhone != null)
		phones.add(makeNewPhone.apply(user).apply(dto.mobilePhone, findPhoneType.apply(MOBILE_PHONE)));
		if (dto.homePhone != null)
		phones.add(makeNewPhone.apply(user).apply(dto.homePhone, findPhoneType.apply(HOME_PHONE)));
		if (dto.workPhone != null)
		phones.add(makeNewPhone.apply(user).apply(dto.workPhone, findPhoneType.apply(WORK_PHONE)));
		return phones;
	}
	
	private static Company makeCompanyFromDTO(CompanyDTO dto, User user) {
		Company company = new Company(dto.company,
										new Date(),
										new Date(),
										null,
										null,
										null);
		
		company.setCompanyAddress(makeCompanyAddressFromDTO(dto.address, company));
		company.setCompanyPhone(makeCompanyPhoneFromDTO(dto.phone, company));
		return company;
	}
	
	private static CompanyAddress makeCompanyAddressFromDTO(AddressDTO dto, Company company) {
		return new CompanyAddress(company, 
									dto.address1,
									dto.address2,
									dto.city,
									dto.state,
									dto.country,
									dto.postalCode,
									new Date(),
									new Date());
	}
	
	private static CompanyPhone makeCompanyPhoneFromDTO(String phone, Company company) {
		return new CompanyPhone(company, phone, new Date(), new Date());
	}
			
			

			
	private static final Function<User, Function<AccountDTO, User>> newUser = 
			user ->
				acct -> 
				
	
		
	public static final Function<AccountDTO, AccountDTO> saveAccount = 
				acct -> acct;
*/}
