package com.akmade.security.repositories;

import java.util.Collection;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.akmade.security.data.Company;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.CompanyPhone;
import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
import com.akmade.security.repositories.SessionRepo.Qry;
import com.akmade.security.repositories.SessionRepo.Txn;
import com.akmade.messaging.security.dto.SecurityDTO;

public class CompanyRepo {
	protected static Function<CompanyPhone, String> makeNewCompanyPhone = 
			p -> p!=null?p.getPhone():null;

	protected static Function<Company, SecurityDTO.Company> makeNewCompanyDTO = 
			company -> SecurityDTO
							.Company.newBuilder()
								.setCompany(company.getCompany())
								.setAddress(AddressRepo.makeNewCompanyAddressDTO.apply(company.getCompanyAddress()))
								.setPhone(makeNewCompanyPhone.apply(company.getCompanyPhone()))
								.build();
			
	protected static Function<UserCompany, SecurityDTO.Company> makeNewCompanyDTOByUserCompany =
			userCompany -> makeNewCompanyDTO.apply(userCompany.getCompany());

			
	protected static Function<Company, Function<String, CompanyPhone>> makeCompanyPhone =
			company ->
				p -> new CompanyPhone(company, p, new Date(), new Date());
				
	protected static Function<CompanyPhone, Function<String, CompanyPhone>> mutateCompanyPhone = 
			oldPhone ->
				dto -> {
					oldPhone.setPhone(dto);
					oldPhone.setLastmodifiedDate(new Date());
					return oldPhone;
				};
	
	protected static Function<Company, Qry<CompanyPhone>> getCompanyPhoneByCompany =
			company ->
				session -> QueryManager.getCompanyPhoneByCompany(company, session);
				
	protected static BiFunction<Company, String, Txn> saveCompanyPhone =
		(company, phoneDTO) ->
			session -> {
				CompanyPhone phone = getCompanyPhoneByCompany.apply(company).apply(session);
				if (phone!=null) 
					CommandManager.saveCompanyPhone.apply(mutateCompanyPhone.apply(phone).apply(phoneDTO)).execute(session);
				else
					CommandManager.saveCompanyPhone.apply(makeCompanyPhone.apply(company).apply(phoneDTO)).execute(session);
		};
		
	protected static Function<Company, Txn> deleteCompanyPhone = 
		company -> 
			session -> {
				CompanyPhone phone = getCompanyPhoneByCompany.apply(company).apply(session);
				if (phone!=null)
					CommandManager.deleteCompanyPhone.apply(phone).execute(session);
				else 
					CommandManager.doNothing.execute(session);
			};
				
	protected static BiFunction<Company, String, Txn> persistCompanyPhone =
			(company, phoneDTO) ->
				session -> {
						if (phoneDTO!=null)
							saveCompanyPhone.apply(company, phoneDTO).execute(session);
						else
							deleteCompanyPhone.apply(company).execute(session);
				};

				
	protected static Function<Company, Function<SecurityDTO.Address, CompanyAddress>> makeCompanyAddress =
			company ->
				dto -> new CompanyAddress(company, 
											dto.getAddress1(), 
											dto.getAddress2(), 
											dto.getCity(), 
											dto.getState(), 
											dto.getCountry(), 
											dto.getPostalCode(), 
											new Date(), 
											new Date());
				
				
	protected static Function<CompanyAddress, Function<SecurityDTO.Address, CompanyAddress>> mutateCompanyAddress = 
			oldAddress ->
				dto -> {
					oldAddress.setAddress1(dto.getAddress1());
					oldAddress.setAddress2(dto.getAddress2());
					oldAddress.setCity(dto.getCity());
					oldAddress.setState(dto.getState());
					oldAddress.setCountry(dto.getCountry());
					oldAddress.setPostalCode(dto.getPostalCode());
					oldAddress.setLastModifiedDate(new Date());
					return oldAddress;
				};
				
	protected static Function<Company, Qry<CompanyAddress>> getCompanyAddressByCompany =
			company ->
				session -> QueryManager.getCompanyAddressByCompany(company, session);
				
	protected static BiFunction<Company, SecurityDTO.Address, Txn> saveCompanyAddress =
			(company, dto) ->
				session -> {
					CompanyAddress address = getCompanyAddressByCompany.apply(company).apply(session);
					if (address!=null)
						CommandManager.saveCompanyAddress.apply(mutateCompanyAddress.apply(address).apply(dto)).execute(session);
					else
						CommandManager.saveCompanyAddress.apply(makeCompanyAddress.apply(company).apply(dto)).execute(session);
			};
			
	protected static Function<Company, Txn> deleteCompanyAddress = 
		company -> 
			session ->{
				CompanyAddress address = getCompanyAddressByCompany.apply(company).apply(session);
				if (address!=null)
						CommandManager.deleteCompanyAddress.apply(address).execute(session);
				else 
						CommandManager.doNothing.execute(session);
				};
	
	protected static BiFunction<Company, SecurityDTO.Address, Txn> persistCompanyAddress =
			(company, addressDTO) ->
				session -> {
						if (addressDTO!=null)
							saveCompanyAddress.apply(company, addressDTO).execute(session);
						else
							deleteCompanyAddress.apply(company).execute(session);
				};
				
				
				
	protected static BiFunction<Company, SecurityDTO.Account, Qry<UserCompany>> makeUserCompany =
			(company, account) -> 
				session -> {
						User user = UserRepo.getDBUser.apply(account).apply(session);
						return user != null?
							new UserCompany(company, 
											user,
											account.getAdministrative(),
											account.getPrimary(),
											account.getBilling()):
							null;
				};
					
	protected static Function<UserCompany, Function<SecurityDTO.Account, UserCompany>> mutateUserCompany =
		userCompany ->
			account -> {
					userCompany.setBilling(account.getBilling());
					userCompany.setAdministrative(account.getAdministrative());
					userCompany.setPrimary(account.getPrimary());
					return userCompany;
			};

	protected static BiFunction<Company, SecurityDTO.Account, Qry<UserCompany>> getUserCompany =
		(company, dto) ->
			session -> {
				UserCompany uCompany = QueryManager.getUserCompany(company.getCompanyId(), dto.getUserId(), session);
				return uCompany!=null?
						mutateUserCompany.apply(uCompany).apply(dto):
						makeUserCompany.apply(company, dto).apply(session);
			};
			
	protected static Function<SecurityDTO.Account, Qry<UserCompany>> getUserCompanyForAccount =
			(accountDTO) ->
				session -> 	{
					Company oldCompany = QueryManager.getCompanyById(accountDTO.getCompany().getCompanyId(), session);
					return getUserCompany.apply(oldCompany, accountDTO).apply(session);
				};
					
	protected static BiFunction<Company, Collection<SecurityDTO.Account>, Qry<Collection<UserCompany>>> makeUserCompanies =
			(company, accounts) -> 
					session -> accounts
									.stream()
									.map(acct -> getUserCompany.apply(company, acct).apply(session))
									.collect(Collectors.toSet());
					
				
	protected static Function<SecurityDTO.Company, Qry<Company>> makeCompany =
			dto -> 
				session -> 
				{ 
					Company company =new Company(dto.getCompany(), 
													new Date(), 
													new Date(),
													new CompanyPhone(),
													new CompanyAddress(),
													null);
				
					company.setCompanyPhone(makeCompanyPhone.apply(company).apply(dto.getPhone()));
					company.setCompanyAddress(makeCompanyAddress.apply(company).apply(dto.getAddress()));
					company.getUserCompanies().addAll(makeUserCompanies.apply(company, dto.getUsersList()).apply(session));
					return company;
			};
				
	protected static BiPredicate<UserCompany, UserCompany> isSameUserCompany = 
			(uc, uc2) ->
				uc.getCompany().getCompanyId().equals(uc2.getCompany().getCompanyId())&&
				uc.getUser().getUserId().equals(uc2.getUser().getUserId());
				
		
	protected static Function<Collection<UserCompany>, Predicate<UserCompany>> containsUserCompany =
			userCompanies ->
				uc -> userCompanies
						.stream()
						.anyMatch(ouc -> isSameUserCompany.test(ouc, uc));							
					
				
	protected static BiFunction<Collection<UserCompany>, Collection<UserCompany>, Txn> deleteUserCompanies =
		(oldUsers, newUsers) ->
			session -> {
				oldUsers
					.stream()
					.filter(uc -> containsUserCompany.apply(newUsers).test(uc)!=true)
					.map(uc -> CommandManager.deleteUserCompany.apply(uc))
					.reduce(CommandManager.doNothing,Txn::andThen)
					.execute(session);
			};
			
	protected static BiFunction<Company, Collection<SecurityDTO.Account>, Txn> persistUserCompanies =
			(company, accounts) ->
				session -> {
					Collection<UserCompany> newUserCompanies = makeUserCompanies.apply(company, accounts).apply(session);
					deleteUserCompanies.apply(company.getUserCompanies(), newUserCompanies)
										.andThen(CommandManager.saveUserCompanies.apply(newUserCompanies)).execute(session);
				};
			
	protected static BiFunction<Company, SecurityDTO.Company, Txn> persistCompany =
		(oldCompany, dto) -> 
			session -> {
			oldCompany.setCompany(dto.getCompany());
			oldCompany.setLastmodifiedDate(new Date());
			CommandManager.saveCompany.apply(oldCompany).execute(session);
		};	
						
	protected static BiFunction<Company, SecurityDTO.Company, Txn> persistOldCompanyTree = 
			(oldCompany, dto) -> 
				session -> persistUserCompanies.apply(oldCompany, dto.getUsersList())
							.andThen(persistCompanyPhone.apply(oldCompany, dto.getPhone()))
							.andThen(persistCompanyAddress.apply(oldCompany, dto.getAddress()))
							.andThen(persistUserCompanies.apply(oldCompany, dto.getUsersList()))
							.andThen(persistCompany.apply(oldCompany, dto)).execute(session);
				
	protected static Function<SecurityDTO.Company, Txn> persistCompanyTree =
			dto -> 
				session -> 
				{
					Company oldCompany = QueryManager.getCompanyById(dto.getCompanyId(), session);
					if (oldCompany==null)
						CommandManager.saveCompanyTree.apply(makeCompany.apply(dto).apply(session)).execute(session);
					else
						persistOldCompanyTree.apply(oldCompany, dto).execute(session);
				};
}
