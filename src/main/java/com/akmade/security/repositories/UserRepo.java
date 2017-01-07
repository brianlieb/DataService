package com.akmade.security.repositories;

import static com.akmade.security.Constants.HOME_PHONE;
import static com.akmade.security.Constants.MOBILE_PHONE;
import static com.akmade.security.Constants.WORK_PHONE;
import static com.akmade.security.util.RepositoryUtility.*;
import static com.akmade.security.Constants.MAILING_ADDRESS;
import static com.akmade.security.Constants.SHIPPING_ADDRESS;

import java.util.Collection;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
import com.akmade.security.data.UserRole;
import com.akmade.security.util.Qry;
import com.akmade.security.util.SessionUtility.CritQuery;
import com.akmade.security.util.Transaction.Txn;
import com.akmade.messaging.security.dto.SecurityDTO;

public class UserRepo {
	private static CritQuery userQuery = 
			session -> session.createCriteria(User.class, "user");
			
	private static CritQuery userRoleQuery = 
			session -> session.createCriteria(UserRole.class, "userRole")
								.createAlias("user", "user")
								.createAlias("role", "role")
								.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	
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
							.andThen(PhoneRepo.savePhones.apply(user.getPhones()))
							.andThen(AddressRepo.saveAddresses.apply(user.getAddresses()))
							.andThen(saveUserRoles.apply(user.getUserRoles()))
							.andThen(CompanyRepo.saveUserCompany.apply(user.getUserCompany()));
			
	protected static Function<Collection<User>, Txn> saveUserTrees =
			users -> users.stream()
						.map(u -> saveUserTree.apply(u))
						.reduce(doNothing,Txn::andThen);

	protected static Function<User, Txn> deleteUser =
			user ->		deleteUserRoles.apply(user.getUserRoles())
							.andThen(AddressRepo.deleteAddresses.apply(user.getAddresses()))
							.andThen(PhoneRepo.deletePhones.apply(user.getPhones()))
							.andThen(CompanyRepo.deleteUserCompany.apply(user.getUserCompany()))
							.andThen(prepareTransaction.apply(delete).apply(user));
	
	protected static Function<Collection<User>, Txn> deleteUsers =
			users -> users.stream()
						.map(u -> deleteUser.apply(u))
						.reduce(doNothing, Txn::andThen);

	protected static Function<String, Qry<User>> getUserByUsername =
		userName -> 
			session -> {
			try {
				return (User)userQuery
								.apply(session)
								.add(Restrictions.eq("username", userName))
								.uniqueResult();
			} catch(Exception e){
				throw logAndThrowError("Error getting user.", e);
			}
		};
	
	protected static Function<Integer, Qry<User>> getUserById =
		userId -> 
			session -> {
				try {
					return (User)userQuery
									.apply(session)
									.add(Restrictions.eq("userId", userId))
									.uniqueResult();
				} catch(Exception e){
					throw logAndThrowError("Error getting user.", e);
				}
			};
	
	protected static Function<Integer, Function<String, Qry<UserRole>>> getUserRole =
		userId -> 
			role -> 
				session -> {
					try {
						return (UserRole)userRoleQuery
												.apply(session)
												.add(Restrictions.eq("user.userId", userId))
												.add(Restrictions.eq("role.role", role))
												.uniqueResult();
					} catch(Exception e){
						throw logAndThrowError("Error getting user.", e);
					}
				};	
	
	@SuppressWarnings("unchecked")
	protected static Qry<Collection<User>> getUsers =
	session -> {
				try {
					return userQuery
							.apply(session)
							.list();
				} catch(Exception e){
					throw logAndThrowError("Error getting users.", e);
				}
			};	
			
			
			
	protected static Function<SecurityDTO.Account, Qry<User>> getDBUser = 
		dto -> 
			session -> getUserById.apply(dto.getUserId()).execute(session);
	
	protected static Predicate<User> isPrimaryContact =
				u -> u.getUserCompany()!=null?u.getUserCompany().isPrimary():false;

	protected static Predicate<User> isAdministrativeContact =
			u -> u.getUserCompany()!=null?u.getUserCompany().isAdministrative():false;
	
	protected static Predicate<User> isBillingContact =
			u -> u.getUserCompany()!=null?u.getUserCompany().isBilling():false;

	protected static Function<User, SecurityDTO.Account> makeAccountDTO = 
			user ->	SecurityDTO.
						Account.newBuilder()
							.setUserName(user.getUsername())
							.setEmail(user.getEmail())
							.setFirstName(user.getFirstName())
							.setMiddleInitial(Character.toString(user.getMiddleInitial()))
							.setLastName(user.getLastName())
							.setShippingAddress(AddressRepo.makeNewAddressDTOByType.apply(SHIPPING_ADDRESS).apply(user.getAddresses()))
							.setMailingAddress(AddressRepo.makeNewAddressDTOByType.apply(MAILING_ADDRESS).apply(user.getAddresses()))
							.setMobilePhone(PhoneRepo.makeNewPhoneDTOByType.apply(MOBILE_PHONE).apply(user.getPhones()))
							.setHomePhone(PhoneRepo.makeNewPhoneDTOByType.apply(HOME_PHONE).apply(user.getPhones()))
							.setWorkPhone(PhoneRepo.makeNewPhoneDTOByType.apply(WORK_PHONE).apply(user.getPhones()))
							.setCompany(user.getUserCompany()!=null?CompanyRepo.makeNewCompanyDTOByUserCompany.apply(user.getUserCompany()):null)
							.setPrimary(isPrimaryContact.test(user))
							.setAdministrative(isAdministrativeContact.test(user))
							.setBilling(isBillingContact.test(user))
							.addAllRoles(RoleRepo.makeNewRoleDTOs.apply(user.getUserRoles()))
							.build();
						
	protected static Function<Collection<User>, Collection<SecurityDTO.Account>> makeAccountDTOs =
			users -> users.parallelStream()
						.map(u -> makeAccountDTO.apply(u))
						.collect(Collectors.toList());
			
	protected static Qry<Collection<SecurityDTO.Account>> getAccountDTOs =
			session -> makeAccountDTOs.apply(getUsers.execute(session));
			
	protected static Function<String, Qry<SecurityDTO.Account>> getAccountDTOByUsername =
			username ->
				session -> makeAccountDTO.apply(getUserByUsername.apply(username).execute(session));
	
	protected static Function<Integer, Qry<SecurityDTO.Account>> getAccountDTOById =
			userId ->
				session -> makeAccountDTO.apply(getUserById.apply(userId).execute(session));
			
	protected static Function<SecurityDTO.Account, Qry<User>> makeNewUser = dto -> session -> {
		User newUser = new User(dto.getUserName(), null, // TODO password?
				dto.getEmail(), dto.getFirstName(), dto.getMiddleInitial().charAt(0), dto.getLastName(), false,
				new Date(), new Date(), null, null, null,
				dto.getCompany() != null ? CompanyRepo.getUserCompanyForAccount.apply(dto).execute(session) : null);

		newUser.getUserRoles().addAll(RoleRepo.makeUserRoles.apply(newUser, dto.getRolesList()).execute(session));
		newUser.getAddresses().addAll(AddressRepo.makeNewAddresses.apply(newUser).apply(dto).execute(session));
		newUser.getPhones().addAll(PhoneRepo.makeNewPhones.apply(newUser).apply(dto).execute(session));
		return newUser;
	};

	protected static BiFunction<UserCompany, UserCompany, Txn> deleteUserCompany = 
		(oldUserCompany, newUserCompany) -> 
			session -> {
				if (CompanyRepo.isSameUserCompany.test(oldUserCompany, newUserCompany))
					CompanyRepo.deleteUserCompany.apply(oldUserCompany).execute(session);
				else 
					doNothing.execute(session);
			};

	protected static BiFunction<User, SecurityDTO.Account, Qry<UserCompany>> makeUserCompany = 
			(user, accountDTO) -> session -> CompanyRepo.getUserCompanyForAccount.apply(accountDTO).execute(session);

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistUserCompany = (
			user, accountDTO) -> session -> {
				UserCompany newUserCompany = makeUserCompany.apply(user, accountDTO).execute(session);
				deleteUserCompany.apply(user.getUserCompany(), newUserCompany)
								.andThen(CompanyRepo.saveUserCompany.apply(newUserCompany))
				.execute(session);
			};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistUser = (oldUser, dto) -> {
		oldUser.setFirstName(dto.getFirstName());
		oldUser.setMiddleInitial(dto.getMiddleInitial().charAt(0));
		oldUser.setLastName(dto.getLastName());
		oldUser.setEmail(dto.getEmail());
		oldUser.setPassword(null); // TODO Forgot to do password stuff
		oldUser.setUsername(dto.getUserName());
		oldUser.setLastmodifiedDate(new Date());
		return saveUser.apply(oldUser);
	};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistOldUserTree = (
			oldUser,
			dto) -> session -> persistUserCompany.apply(oldUser, dto)
								.andThen(PhoneRepo.persistPhones.apply(oldUser, dto))
								.andThen(AddressRepo.persistAddresses.apply(oldUser, dto))
								.andThen(RoleRepo.persistUserRoles.apply(oldUser, dto.getRolesList()))
								.andThen(persistUser.apply(oldUser, dto))
							.execute(session);;

	protected static Function<SecurityDTO.Account, Txn> persistUserTree = 
		dto -> 
			session -> {
				User oldUser = getUserById.apply(dto.getUserId()).execute(session);
				if (oldUser == null)
					saveUserTree.apply(makeNewUser.apply(dto).execute(session)).execute(session);
				else
					persistOldUserTree.apply(oldUser, dto).execute(session);
	};

}
