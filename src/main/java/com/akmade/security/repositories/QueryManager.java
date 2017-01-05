package com.akmade.security.repositories;

import java.util.Collection;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

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
import com.akmade.security.repositories.SessionRepo.CritQuery;

public class QueryManager extends DataSession {
	
	private static CritQuery phoneTypeQuery = 
		session -> session.createCriteria(PhoneType.class, "phoneType");
	
	private static CritQuery addressTypeQuery =
			session -> session.createCriteria(AddressType.class, "addressType");

	private static CritQuery roleTypeQuery = 
			session -> session.createCriteria(RoleType.class, "roleType");
			
	private static CritQuery phoneQuery = 
			session -> session.createCriteria(Phone.class, "phone")
											.createAlias("phoneType", "phoneType")
											.createAlias("user", "user")
											.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	
	private static CritQuery addressQuery = 
			session -> session.createCriteria(Address.class, "address")
												.createAlias("addressType", "addressType")
												.createAlias("user", "user")
												.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

	private static CritQuery userQuery = 
			session -> session.createCriteria(User.class, "user");
			
	private static CritQuery roleQuery = 
			session -> session.createCriteria(Role.class, "role");
							
	private static CritQuery userCompanyQuery = 
			session -> session.createCriteria(UserCompany.class, "userCompany")
								.createAlias("user", "user")
								.createAlias("company", "company")
								.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			
	private static CritQuery userRoleQuery = 
			session -> session.createCriteria(UserRole.class, "userRole")
								.createAlias("user", "user")
								.createAlias("role", "role")
								.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

	private static CritQuery companyQuery =
			session -> session.createCriteria(Company.class, "company")
												.createAlias("companyAddresses", "addresses")
												.createAlias("companyPhones", "phones")
												.createAlias("userCompanies", "userCompanies")
												.createAlias("userCompanies.user", "user")
												.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	
	@SuppressWarnings("unchecked")
	protected static Collection<Company> getCompanies(Session session) {
		try {
			return companyQuery
					.apply(session)
					.list();
		} catch (Exception e) {
			throw logAndThrowError("Error getting companies. " + e.getMessage());
		} 
	}
	
	protected static Company getCompanyById(Integer companyId, Session session) {
		try {
			return (Company)companyQuery
							.apply(session)
							.add(Restrictions.eq("companyId", companyId))
							.uniqueResult();
		} catch (Exception e) {
			throw logAndThrowError("Error getting company. " + e.getMessage());
		} 
	}
	
	
	protected static CompanyPhone getCompanyPhoneByCompany(Company company, Session session) {
		try {
			Company com = (Company)companyQuery
										.apply(session)
										.add(Restrictions.eq("companyId", company.getCompanyId()))
										.uniqueResult();
		
			return com!=null?com.getCompanyPhone():null;
			
		} catch (Exception e) {
			throw logAndThrowError("Error getting company. " + e.getMessage());
		} 
	}

	
	protected static CompanyAddress getCompanyAddressByCompany(Company company, Session session) {
		try {
			Company com = (Company)companyQuery
										.apply(session)
										.add(Restrictions.eq("companyId", company.getCompanyId()))
										.uniqueResult();
		
			return com!=null?com.getCompanyAddress():null;
			
		} catch (Exception e) {
			throw logAndThrowError("Error getting company. " + e.getMessage());
		} 
	}
	
	@SuppressWarnings("unchecked")
	protected static Collection<PhoneType> getPhoneTypes(Session session) {
		try {
			return phoneTypeQuery
					.apply(session)
					.list();
		} catch (Exception e) {
			throw logAndThrowError("Error getting phone types. " + e.getMessage());
		} 
	}
	
	protected static PhoneType getPhoneTypeByType(String type, Session session) {
		try {
			return (PhoneType) phoneTypeQuery
								.apply(session)
								.add( Restrictions.eq("type", type))
								.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting phone type.", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected static Collection<AddressType> getAddressTypes(Session session) {
		try {
			return addressTypeQuery
						.apply(session)
						.list();
		} catch (Exception e) {
			throw logAndThrowError("Error getting address types. " + e.getMessage());
		} 
	}
	
	
	protected static AddressType getAddressTypeByType(String type, Session session) {
		logger.info("Looking for address Type " +  type +".");
		try {
			return (AddressType) addressTypeQuery
									.apply(session)
									.add( Restrictions.eq("type", type))
									.uniqueResult();
		} catch(Exception e){
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected static Collection<RoleType> getRoleTypes(Session session) {
		try {
			return roleTypeQuery
					.apply(session)
					.list();
		} catch (Exception e) {
			throw logAndThrowError("Error getting role types. " + e.getMessage());
		} 
	}
	
	protected static RoleType getRoleTypeByType(String type, Session session) {
		try {
			return (RoleType) roleTypeQuery
					.apply(session)
					.add( Restrictions.eq("type", type))
					.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting role type.", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected static Collection<Phone> getPhonesByUser(String userName, Session session) {
		try {
			return phoneQuery
					.apply(session)
					.add(Restrictions.eq("user.username", userName))
					.list();
		} catch(Exception e) {
			throw logAndThrowError("Error getting phones.", e);
		}
	}
	
	protected static Phone getPhoneByUserType(User user, PhoneType type, Session session) {
		try {
			return (Phone)phoneQuery
							.apply(session)
							.add(Restrictions.eq("user.userId", user.getUserId()))
							.add(Restrictions.eq("phoneType.type", type.getType()))
							.uniqueResult();
		} catch(Exception e) {
			throw logAndThrowError("Error getting phone.", e);
		}
	}


	@SuppressWarnings("unchecked")
	protected static Collection<Address> getAddressesByUser(String userName, Session session) {
		try {
			return addressQuery
						.apply(session)
						.add(Restrictions.eq("user.username", userName))
						.list();
		} catch(Exception e) {
			throw logAndThrowError("Error getting addresses.", e);
		}
	}
	
	
	protected static Address getAddressByUserType(User user, AddressType type, Session session) {
		try {
			return (Address)addressQuery
							.apply(session)
							.add(Restrictions.eq("user.userId", user.getUserId()))
							.add(Restrictions.eq("addressType.type", type.getType()))
							.uniqueResult();
		} catch(Exception e) {
			throw logAndThrowError("Error getting address.", e);
		}
	}

	
	
	protected static User getUserByUsername(String userName, Session session) {
		try {
			return (User)userQuery
							.apply(session)
							.add(Restrictions.eq("username", userName))
							.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting user.", e);
		}
	}
	

	protected static User getUserById(Integer userId, Session session) {
		try {
			return (User)userQuery
							.apply(session)
							.add(Restrictions.eq("userId", userId))
							.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting user.", e);
		}
	}
	
	
	protected static Role getRole(String role, Session session) {
		try {
			return (Role)roleQuery
							.apply(session)
							.add(Restrictions.eq("role", role))
							.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting role.", e);
		}
	}
	
	protected static UserCompany getUserCompany(Integer companyId, Integer userId, Session session) {
		try {
			return (UserCompany)userCompanyQuery
									.apply(session)
									.add(Restrictions.eq("user.userId", userId))
									.add(Restrictions.eq("company.companyId", companyId))
									.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting user.", e);
		}
	}
	
	
	protected static UserRole getUserRole(Integer userId, String role, Session session) {
		try {
			return (UserRole)userRoleQuery
									.apply(session)
									.add(Restrictions.eq("user.userId", userId))
									.add(Restrictions.eq("role.role", role))
									.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting user.", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected static Collection<User> getUsers(Session session) {
		try {
			return userQuery
					.apply(session)
					.list();
		} catch(Exception e){
			throw logAndThrowError("Error getting users.", e);
		}
	}
}
