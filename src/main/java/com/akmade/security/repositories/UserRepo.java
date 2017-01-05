package com.akmade.security.repositories;

import static com.akmade.security.Constants.HOME_PHONE;
import static com.akmade.security.Constants.MOBILE_PHONE;
import static com.akmade.security.Constants.WORK_PHONE;
import static com.akmade.security.Constants.MAILING_ADDRESS;
import static com.akmade.security.Constants.SHIPPING_ADDRESS;

import java.util.Collection;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
import com.akmade.security.repositories.SessionRepo.Qry;
import com.akmade.security.repositories.SessionRepo.Txn;
import com.akmade.messaging.security.dto.SecurityDTO;

public class UserRepo {
	
	protected static Function<SecurityDTO.Account, Qry<User>> getDBUser = dto -> session -> QueryManager
			.getUserById(dto.getUserId(), session);
	
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
			session -> makeAccountDTOs.apply(QueryManager.getUsers(session));
			
	protected static Function<String, Qry<SecurityDTO.Account>> getAccountDTOByUsername =
			username ->
				session -> makeAccountDTO.apply(QueryManager.getUserByUsername(username, session));
	
	protected static Function<Integer, Qry<SecurityDTO.Account>> getAccountDTOById =
			userId ->
				session -> makeAccountDTO.apply(QueryManager.getUserById(userId, session));
			
	protected static Function<SecurityDTO.Account, Qry<User>> makeNewUser = dto -> session -> {
		User newUser = new User(dto.getUserName(), null, // TODO password?
				dto.getEmail(), dto.getFirstName(), dto.getMiddleInitial().charAt(0), dto.getLastName(), false,
				new Date(), new Date(), null, null, null,
				dto.getCompany() != null ? CompanyRepo.getUserCompanyForAccount.apply(dto).apply(session) : null);

		newUser.getUserRoles().addAll(RoleRepo.makeUserRoles.apply(newUser, dto.getRolesList()).apply(session));
		newUser.getAddresses().addAll(AddressRepo.makeNewAddresses.apply(newUser).apply(dto).apply(session));
		newUser.getPhones().addAll(PhoneRepo.makeNewPhones.apply(newUser).apply(dto).apply(session));
		return newUser;
	};

	protected static BiFunction<UserCompany, UserCompany, Txn> deleteUserCompany = 
		(oldUserCompany, newUserCompany) -> 
			session -> {
				if (CompanyRepo.isSameUserCompany.test(oldUserCompany, newUserCompany))
					CommandManager.deleteUserCompany.apply(oldUserCompany).execute(session);
				else 
					CommandManager.doNothing.execute(session);
			};

	protected static BiFunction<User, SecurityDTO.Account, Qry<UserCompany>> makeUserCompany = 
			(user, accountDTO) -> session -> CompanyRepo.getUserCompanyForAccount.apply(accountDTO).apply(session);

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistUserCompany = (
			user, accountDTO) -> session -> {
				UserCompany newUserCompany = makeUserCompany.apply(user, accountDTO).apply(session);
				deleteUserCompany.apply(user.getUserCompany(), newUserCompany)
								.andThen(CommandManager.saveUserCompany.apply(newUserCompany))
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
		return CommandManager.saveUser.apply(oldUser);
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
				User oldUser = QueryManager.getUserById(dto.getUserId(), session);
				if (oldUser == null)
					CommandManager.saveUserTree.apply(makeNewUser.apply(dto).apply(session)).execute(session);
				else
					persistOldUserTree.apply(oldUser, dto).execute(session);
	};

}
