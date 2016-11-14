package com.akmade.security.repositories;

import java.util.Collection;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.Session;

import com.akmade.security.data.Company;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.CompanyPhone;
import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
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
	
	protected static Function<Company, Function<Session, CompanyPhone>> getCompanyPhoneByCompany =
			company ->
				session -> QueryManager.getCompanyPhoneByCompany(company, session);
				
	protected static BiFunction<Company, String, Function<Session, Consumer<Session>>> saveCompanyPhone =
		(company, phoneDTO) ->
			session -> {
				CompanyPhone phone = getCompanyPhoneByCompany.apply(company).apply(session);
				return phone!=null?
						CommandManager.saveCompanyPhone.apply(mutateCompanyPhone.apply(phone).apply(phoneDTO)):
						CommandManager.saveCompanyPhone.apply(makeCompanyPhone.apply(company).apply(phoneDTO));
		};
		
	protected static Function<Company, Function<Session, Consumer<Session>>> deleteCompanyPhone = 
		company -> 
			session ->{
				CompanyPhone phone = getCompanyPhoneByCompany.apply(company).apply(session);
				return phone!=null?
						CommandManager.deleteCompanyPhone.apply(phone):
						CommandManager.doNothing;
			};
				
	protected static BiFunction<Company, String, Function<Session, Consumer<Session>>> persistCompanyPhone =
			(company, phoneDTO) ->
				session -> {
						return phoneDTO!=null?
							saveCompanyPhone.apply(company, phoneDTO).apply(session):
							deleteCompanyPhone.apply(company).apply(session);
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
				
	protected static Function<Company, Function<Session, CompanyAddress>> getCompanyAddressByCompany =
			company ->
				session -> QueryManager.getCompanyAddressByCompany(company, session);
				
	protected static BiFunction<Company, SecurityDTO.Address, Function<Session, Consumer<Session>>> saveCompanyAddress =
			(company, dto) ->
				session -> {
					CompanyAddress address = getCompanyAddressByCompany.apply(company).apply(session);
					return address!=null?
							CommandManager.saveCompanyAddress.apply(mutateCompanyAddress.apply(address).apply(dto)):
							CommandManager.saveCompanyAddress.apply(makeCompanyAddress.apply(company).apply(dto));
			};
			
	protected static Function<Company, Function<Session, Consumer<Session>>> deleteCompanyAddress = 
		company -> 
			session ->{
				CompanyAddress address = getCompanyAddressByCompany.apply(company).apply(session);
				return address!=null?
						CommandManager.deleteCompanyAddress.apply(address):
						CommandManager.doNothing;
				};
	
	protected static BiFunction<Company, SecurityDTO.Address, Function<Session, Consumer<Session>>> persistCompanyAddress =
			(company, addressDTO) ->
				session -> {
						return addressDTO!=null?
							saveCompanyAddress.apply(company, addressDTO).apply(session):
							deleteCompanyAddress.apply(company).apply(session);
				};
				
				
				
	protected static BiFunction<Company, SecurityDTO.Account, Function<Session, UserCompany>> makeUserCompany =
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

	protected static BiFunction<Company, SecurityDTO.Account, Function<Session, UserCompany>> getUserCompany =
		(company, dto) ->
			session -> {
				UserCompany uCompany = QueryManager.getUserCompany(company.getCompanyId(), dto.getUserId(), session);
				return uCompany!=null?
						mutateUserCompany.apply(uCompany).apply(dto):
						makeUserCompany.apply(company, dto).apply(session);
			};
			
	protected static Function<SecurityDTO.Account, Function<Session, UserCompany>> getUserCompanyForAccount =
			(accountDTO) ->
				session -> 	{
					Company oldCompany = QueryManager.getCompanyById(accountDTO.getCompany().getCompanyId(), session);
					return getUserCompany.apply(oldCompany, accountDTO).apply(session);
				};
					
	protected static BiFunction<Company, Collection<SecurityDTO.Account>, Function<Session, Collection<UserCompany>>> makeUserCompanies =
			(company, accounts) -> 
					session -> accounts
									.stream()
									.map(acct -> getUserCompany.apply(company, acct).apply(session))
									.collect(Collectors.toSet());
					
				
	protected static Function<SecurityDTO.Company, Function<Session, Company>> makeCompany =
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
					
				
	protected static BiFunction<Collection<UserCompany>, Collection<UserCompany>, Consumer<Session>> deleteUserCompanies =
		(oldUsers, newUsers) ->
			session -> {
				oldUsers
					.stream()
					.filter(uc -> containsUserCompany.apply(newUsers).test(uc)!=true)
					.map(uc -> CommandManager.deleteUserCompany.apply(uc))
					.collect(Collectors.toList());
			};
			
	protected static BiFunction<Company, Collection<SecurityDTO.Account>, Function<Session, Consumer<Session>>> persistUserCompanies =
			(company, accounts) ->
				session -> {
					Collection<UserCompany> newUserCompanies = makeUserCompanies.apply(company, accounts).apply(session);
					return	deleteUserCompanies.apply(company.getUserCompanies(), newUserCompanies)
								.andThen(CommandManager.saveUserCompanies.apply(newUserCompanies));
				};
			
	protected static BiFunction<Company, SecurityDTO.Company, Consumer<Session>> persistCompany =
		(oldCompany, dto) -> {
			oldCompany.setCompany(dto.getCompany());
			oldCompany.setLastmodifiedDate(new Date());
			return CommandManager.saveCompany.apply(oldCompany);
		};	
						
	protected static BiFunction<Company, SecurityDTO.Company, Function<Session, Consumer<Session>>> persistOldCompanyTree = 
			(oldCompany, dto) -> 
				session -> persistUserCompanies.apply(oldCompany, dto.getUsersList()).apply(session)
							.andThen(persistCompanyPhone.apply(oldCompany, dto.getPhone()).apply(session))
							.andThen(persistCompanyAddress.apply(oldCompany, dto.getAddress()).apply(session))
							.andThen(persistUserCompanies.apply(oldCompany, dto.getUsersList()).apply(session))
							.andThen(persistCompany.apply(oldCompany, dto));
					
	protected static Function<SecurityDTO.Company, Function<Session, Consumer<Session>>> persistCompanyTree =
			dto -> 
				session -> 
				{
					Company oldCompany = QueryManager.getCompanyById(dto.getCompanyId(), session);
					return oldCompany==null?
						CommandManager.saveCompanyTree.apply(makeCompany.apply(dto).apply(session)):
						persistOldCompanyTree.apply(oldCompany, dto).apply(session);
				};
}
