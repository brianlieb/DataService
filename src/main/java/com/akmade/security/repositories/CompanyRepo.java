package com.akmade.security.repositories;

import static com.akmade.security.util.RepositoryUtility.*;

import java.util.Collection;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.akmade.security.data.Company;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.CompanyPhone;
import com.akmade.security.data.User;
import com.akmade.security.data.UserCompany;
import com.akmade.security.util.Qry;
import com.akmade.security.util.SessionUtility.CritQuery;
import com.akmade.security.util.Transaction.Txn;
import com.akmade.messaging.security.dto.SecurityDTO;


public class CompanyRepo {
	
	private static CritQuery userCompanyQuery = 
			session -> session.createCriteria(UserCompany.class, "userCompany")
								.createAlias("user", "user")
								.createAlias("company", "company")
								.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			

	private static CritQuery companyQuery =
			session -> session.createCriteria(Company.class, "company")
												.createAlias("companyAddresses", "addresses")
												.createAlias("companyPhones", "phones")
												.createAlias("userCompanies", "userCompanies")
												.createAlias("userCompanies.user", "user")
												.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			
			
	@SuppressWarnings("unchecked")
	protected static Qry<Collection<Company>> getCompanies = 
		session -> {
			try {
				return companyQuery
						.apply(session)
						.list();
			} catch (Exception e) {
				throw logAndThrowError("Error getting companies. " + e.getMessage());
			} 
		};
	
	protected static Function<Integer, Qry<Company>> getCompanyById = 
		companyId ->
			session -> {
				try {
					return (Company)companyQuery
									.apply(session)
									.add(Restrictions.eq("companyId", companyId))
									.uniqueResult();
				} catch (Exception e) {
					throw logAndThrowError("Error getting company. " + e.getMessage());
				} 
			};
	
	
	protected static Function<Company, Qry<CompanyPhone>> getCompanyPhoneByCompany = 
		company ->
			session -> {
				try {
					Company com = (Company)companyQuery
												.apply(session)
												.add(Restrictions.eq("companyId", company.getCompanyId()))
												.uniqueResult();
				
					return com!=null?com.getCompanyPhone():null;
					
				} catch (Exception e) {
					throw logAndThrowError("Error getting company. " + e.getMessage());
				} 
			};

	
	protected static Function<Company, Qry<CompanyAddress>> getCompanyAddressByCompany =
		company ->
			session -> {
				try {
					Company com = (Company)companyQuery
												.apply(session)
												.add(Restrictions.eq("companyId", company.getCompanyId()))
												.uniqueResult();
				
					return com!=null?com.getCompanyAddress():null;
					
				} catch (Exception e) {
					throw logAndThrowError("Error getting company. " + e.getMessage());
				} 
			};
	
	
	protected static Function<Integer, Function<Integer, Qry<UserCompany>>> getUserCompany =
		companyId -> 
			userId ->
				session -> {
					try {
						return (UserCompany)userCompanyQuery
												.apply(session)
												.add(Restrictions.eq("user.userId", userId))
												.add(Restrictions.eq("company.companyId", companyId))
												.uniqueResult();
					} catch(Exception e){
						throw logAndThrowError("Error getting user.", e);
					}
				};
	
	
	protected static Function<CompanyAddress, Txn> deleteAddress =
			companyAddress -> prepareTransaction.apply(delete).apply(companyAddress);
			
	protected static Function<CompanyAddress, Txn> saveAddress =
			companyAddress -> prepareTransaction.apply(save).apply(companyAddress);
			
			
	protected static Function<CompanyPhone, Txn> deletePhone =
			companyPhone -> prepareTransaction.apply(delete).apply(companyPhone);
							
	protected static Function<UserCompany, Txn> deleteUserCompany = 
			userCompany ->	prepareTransaction.apply(delete).apply(userCompany);
			
	protected static Function<Collection<UserCompany>, Txn> deleteUserCompanies = 
			userCompanies -> prepareTransaction(delete, userCompanies);
	
	protected static Function<Company, Txn> deleteCompanyTree = 
			company ->	deleteAddress.apply(company.getCompanyAddress())
											.andThen(deletePhone.apply(company.getCompanyPhone()))
											.andThen(deleteUserCompanies.apply(company.getUserCompanies()))
											.andThen(prepareTransaction.apply(delete).apply(company));
			
	protected static Function<Collection<Company>, Txn> deleteCompanyTrees =
			companies -> companies
							.stream()
							.map(c -> deleteCompanyTree.apply(c))
							.reduce(doNothing,Txn::andThen);

								
	protected static Function<CompanyPhone, Txn> savePhone =
			companyPhone -> prepareTransaction.apply(save).apply(companyPhone);
			
	protected static Function<UserCompany, Txn> saveUserCompany = 
			companyUser ->	prepareTransaction.apply(save).apply(companyUser);
										
	protected static Function<Collection<UserCompany>, Txn> saveUserCompanies = 
			companyUsers ->	prepareTransaction(save, companyUsers);

	protected static Function<Company, Txn> saveCompany =
			company -> prepareTransaction.apply(save).apply(company);

	protected static Function<Company, Txn> saveCompanyTree = 
			company ->  saveCompany.apply(company)
							.andThen(saveAddress.apply(company.getCompanyAddress()))
							.andThen(savePhone.apply(company.getCompanyPhone()))
							.andThen(saveUserCompanies.apply(company.getUserCompanies()));
	

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
	
	protected static BiFunction<Company, String, Txn> saveCompanyPhone =
		(company, phoneDTO) ->
			session -> {
				CompanyPhone phone = getCompanyPhoneByCompany.apply(company).execute(session);
				if (phone!=null) 
					savePhone.apply(mutateCompanyPhone.apply(phone).apply(phoneDTO)).execute(session);
				else
					savePhone.apply(makeCompanyPhone.apply(company).apply(phoneDTO)).execute(session);
		};
		
	protected static Function<Company, Txn> deleteCompanyPhone = 
		company -> 
			session -> {
				CompanyPhone phone = getCompanyPhoneByCompany.apply(company).execute(session);
				if (phone!=null)
					deletePhone.apply(phone).execute(session);
				else 
					doNothing.execute(session);
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
				
	protected static BiFunction<Company, SecurityDTO.Address, Txn> saveCompanyAddress =
			(company, dto) ->
				session -> {
					CompanyAddress address = getCompanyAddressByCompany.apply(company).execute(session);
					if (address!=null)
						saveAddress.apply(mutateCompanyAddress.apply(address).apply(dto)).execute(session);
					else
						saveAddress.apply(makeCompanyAddress.apply(company).apply(dto)).execute(session);
			};
			
	protected static Function<Company, Txn> deleteCompanyAddress = 
		company -> 
			session ->{
				CompanyAddress address = getCompanyAddressByCompany.apply(company).execute(session);
				if (address!=null)
						deleteAddress.apply(address).execute(session);
				else 
						doNothing.execute(session);
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
						User user = UserRepo.getDBUser.apply(account).execute(session);
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

	protected static BiFunction<Company, SecurityDTO.Account, Qry<UserCompany>> getNewUserCompany =
		(company, dto) ->
			session -> {
				UserCompany uCompany = getUserCompany.apply(company.getCompanyId()).apply(dto.getUserId()).execute(session);
				return uCompany!=null?
						mutateUserCompany.apply(uCompany).apply(dto):
						makeUserCompany.apply(company, dto).execute(session);
			};
			
	protected static Function<SecurityDTO.Account, Qry<UserCompany>> getUserCompanyForAccount =
			(accountDTO) ->
				session -> 	{
					Company oldCompany = getCompanyById.apply(accountDTO.getCompany().getCompanyId()).execute(session);
					return getUserCompany.apply(oldCompany.getCompanyId()).apply(accountDTO.getUserId()).execute(session);
				};
					
	protected static BiFunction<Company, Collection<SecurityDTO.Account>, Qry<Collection<UserCompany>>> makeUserCompanies =
			(company, accounts) -> 
					session -> accounts
									.stream()
									.map(acct -> getNewUserCompany.apply(company, acct).execute(session))
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
					company.getUserCompanies().addAll(makeUserCompanies.apply(company, dto.getUsersList()).execute(session));
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
					
				
	protected static BiFunction<Collection<UserCompany>, Collection<UserCompany>, Txn> deleteUsersCompanies =
		(oldUsers, newUsers) ->
			session -> {
				oldUsers
					.stream()
					.filter(uc -> containsUserCompany.apply(newUsers).test(uc)!=true)
					.map(uc -> deleteUserCompany.apply(uc))
					.reduce(doNothing,Txn::andThen)
					.execute(session);
			};
			
	protected static BiFunction<Company, Collection<SecurityDTO.Account>, Txn> persistUserCompanies =
			(company, accounts) ->
				session -> {
					Collection<UserCompany> newUserCompanies = makeUserCompanies.apply(company, accounts).execute(session);
					deleteUsersCompanies.apply(company.getUserCompanies(), newUserCompanies)
										.andThen(saveUserCompanies.apply(newUserCompanies)).execute(session);
				};
			
	protected static BiFunction<Company, SecurityDTO.Company, Txn> persistCompany =
		(oldCompany, dto) -> 
			session -> {
			oldCompany.setCompany(dto.getCompany());
			oldCompany.setLastmodifiedDate(new Date());
			saveCompany.apply(oldCompany).execute(session);
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
					Company oldCompany = getCompanyById.apply(dto.getCompanyId()).execute(session);
					if (oldCompany==null)
						saveCompanyTree.apply(makeCompany.apply(dto).execute(session)).execute(session);
					else
						persistOldCompanyTree.apply(oldCompany, dto).execute(session);
				};
}
