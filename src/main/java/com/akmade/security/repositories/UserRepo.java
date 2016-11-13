package com.akmade.security.repositories;

import static com.akmade.security.Constants.HOME_PHONE;
import static com.akmade.security.Constants.MAILING_ADDRESS;
import static com.akmade.security.Constants.MOBILE_PHONE;
import static com.akmade.security.Constants.SHIPPING_ADDRESS;
import static com.akmade.security.Constants.WORK_PHONE;

import java.util.Collection;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.Session;

import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
import com.akmade.security.dto.DataTransferObjects;

public class UserRepo {
	
	protected static Function<DataTransferObjects.Account, Function<Session, User>> getDBUser = dto -> session -> QueryManager
			.getUserById(dto.getUserId(), session);
	
	protected static Predicate<User> isPrimaryContact =
				u -> u.getUserCompany()!=null?u.getUserCompany().isPrimary():false;

	protected static Predicate<User> isAdministrativeContact =
			u -> u.getUserCompany()!=null?u.getUserCompany().isAdministrative():false;
	
	protected static Predicate<User> isBillingContact =
			u -> u.getUserCompany()!=null?u.getUserCompany().isBilling():false;

	protected static Function<User, DataTransferObjects.Account> makeAccountDTO = 
			user ->	DataTransferObjects.
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
						
	protected static Function<Collection<User>, Collection<DataTransferObjects.Account>> makeAccountDTOs =
			users -> users.parallelStream()
						.map(u -> makeAccountDTO.apply(u))
						.collect(Collectors.toList());
			
	protected static Function<Session, Collection<DataTransferObjects.Account>> getAccountDTOs =
			session -> makeAccountDTOs.apply(QueryManager.getUsers(session));
			
	protected static Function<String, Function<Session, DataTransferObjects.Account>> getAccountDTOByUsername =
			username ->
				session -> makeAccountDTO.apply(QueryManager.getUserByUsername(username, session));
	
	protected static Function<Integer, Function<Session, DataTransferObjects.Account>> getAccountDTOById =
			userId ->
				session -> makeAccountDTO.apply(QueryManager.getUserById(userId, session));
			
	protected static Function<DataTransferObjects.Account, Function<Session, User>> makeNewUser = dto -> session -> {
		User newUser = new User(dto.getUserName(), null, // TODO password?
				dto.getEmail(), dto.getFirstName(), dto.getMiddleInitial().charAt(0), dto.getLastName(), false,
				new Date(), new Date(), null, null, null,
				dto.getCompany() != null ? CompanyRepo.getUserCompanyForAccount.apply(dto).apply(session) : null);

		newUser.getUserRoles().addAll(RoleRepo.makeUserRoles.apply(newUser, dto.getRolesList()).apply(session));
		newUser.getAddresses().addAll(AddressRepo.makeNewAddresses.apply(newUser).apply(dto).apply(session));
		newUser.getPhones().addAll(PhoneRepo.makeNewPhones.apply(newUser).apply(dto).apply(session));
		return newUser;
	};

	protected static BiFunction<UserCompany, UserCompany, Consumer<Session>> deleteUserCompany = (oldUserCompany,
			newUserCompany) -> CompanyRepo.isSameUserCompany.test(oldUserCompany, newUserCompany)
					? CommandManager.deleteUserCompany.apply(oldUserCompany) : CommandManager.doNothing;

	protected static BiFunction<User, DataTransferObjects.Account, Function<Session, UserCompany>> makeUserCompany = 
			(user, accountDTO) -> session -> CompanyRepo.getUserCompanyForAccount.apply(accountDTO).apply(session);

	protected static BiFunction<User, DataTransferObjects.Account, Function<Session, Consumer<Session>>> persistUserCompany = (
			user, accountDTO) -> session -> {
				UserCompany newUserCompany = makeUserCompany.apply(user, accountDTO).apply(session);
				return deleteUserCompany.apply(user.getUserCompany(), newUserCompany)
						.andThen(CommandManager.saveUserCompany.apply(newUserCompany));
			};

	protected static BiFunction<User, DataTransferObjects.Account, Consumer<Session>> persistUser = (oldUser, dto) -> {
		oldUser.setFirstName(dto.getFirstName());
		oldUser.setMiddleInitial(dto.getMiddleInitial().charAt(0));
		oldUser.setLastName(dto.getLastName());
		oldUser.setEmail(dto.getEmail());
		oldUser.setPassword(null); // TODO Forgot to do password stuff
		oldUser.setUsername(dto.getUserName());
		oldUser.setLastmodifiedDate(new Date());
		return CommandManager.saveUser.apply(oldUser);
	};

	protected static BiFunction<User, DataTransferObjects.Account, Function<Session, Consumer<Session>>> persistOldUserTree = (
			oldUser,
			dto) -> session -> persistUserCompany.apply(oldUser, dto).apply(session)
					.andThen(PhoneRepo.persistPhones.apply(oldUser, dto).apply(session))
					.andThen(AddressRepo.persistAddresses.apply(oldUser, dto).apply(session))
					.andThen(RoleRepo.persistUserRoles.apply(oldUser, dto.getRolesList()).apply(session))
					.andThen(persistUser.apply(oldUser, dto));

	protected static Function<DataTransferObjects.Account, Function<Session, Consumer<Session>>> persistUserTree = dto -> session -> {
		User oldUser = QueryManager.getUserById(dto.getUserId(), session);
		return oldUser == null ? CommandManager.saveUserTree.apply(makeNewUser.apply(dto).apply(session))
				: persistOldUserTree.apply(oldUser, dto).apply(session);
	};

}