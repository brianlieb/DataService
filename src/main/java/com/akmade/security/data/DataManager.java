package com.akmade.security.data;

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import com.akmade.security.data.AddressType;
import com.akmade.security.data.PhoneType;
import com.akmade.security.data.RoleType;

public class DataManager extends DataSession {
	
	private static DetachedCriteria phoneTypeQuery = DetachedCriteria.forClass(PhoneType.class);
	private static DetachedCriteria addressTypeQuery = DetachedCriteria.forClass(AddressType.class);
	
	private static DetachedCriteria roleTypeQuery = DetachedCriteria.forClass(RoleType.class);
	
	private static DetachedCriteria phoneQuery = DetachedCriteria.forClass(Phone.class)
											.createAlias("phoneType", "phoneType")
											.createAlias("user", "user")
											.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	
	private static DetachedCriteria addressQuery = DetachedCriteria.forClass(Address.class)
												.createAlias("addressType", "addressType")
												.createAlias("user", "user")
												.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

	private static DetachedCriteria userQuery = DetachedCriteria.forClass(User.class);
	
	private static DetachedCriteria companyQuery = DetachedCriteria.forClass(Company.class)
												.createAlias("companyAddresses", "addresses")
												.createAlias("companyPhones", "phones")
												.createAlias("userCompanies", "userCompanies")
												.createAlias("userCompanies.user", "user")
												.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	
	@SuppressWarnings("unchecked")
	protected static Collection<Company> getCompanies(Session session) {
		try {
			return companyQuery
					.getExecutableCriteria(session)
					.list();
		} catch (Exception e) {
			throw logAndThrowError("Error getting companies. " + e.getMessage());
		} 
	}
	
	@SuppressWarnings("unchecked")
	protected static Collection<PhoneType> getPhoneTypes(Session session) {
		try {
			return phoneTypeQuery
					.getExecutableCriteria(session)
					.list();
		} catch (Exception e) {
			throw logAndThrowError("Error getting phone types. " + e.getMessage());
		} 
	}
	
	protected static PhoneType getPhoneTypeByType(String type, Session session) {
		try {
			return (PhoneType) phoneTypeQuery
								.add( Restrictions.eq("type", type))
								.getExecutableCriteria(session)
								.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting phone type.", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected static Collection<AddressType> getAddressTypes(Session session) {
		try {
			return addressTypeQuery
					.getExecutableCriteria(session)
					.list();
		} catch (Exception e) {
			throw logAndThrowError("Error getting address types. " + e.getMessage());
		} 
	}
	
	
	protected static AddressType getAddressTypeByType(String type, Session session) {
		logger.info("Looking for address Type " +  type +".");
		try {
			return (AddressType) addressTypeQuery
									.add( Restrictions.eq("type", type))
									.getExecutableCriteria(session)
									.uniqueResult();
		} catch(Exception e){
			return null;
		}
	}
	
	protected static Integer getAddressTypeMaxId(Session session) {
		Integer i = null;
		
		try {
			i = (Integer) addressTypeQuery
							.setProjection(Projections.max("addressTypeId"))
							.getExecutableCriteria(session)
							.uniqueResult();
		} catch (Exception e) {} 
		return i!=null?i:0;
	}

	@SuppressWarnings("unchecked")
	protected static Collection<RoleType> getRoleTypes(Session session) {
		try {
			return roleTypeQuery
					.getExecutableCriteria(session)
					.list();
		} catch (Exception e) {
			throw logAndThrowError("Error getting role types. " + e.getMessage());
		} 
	}
	
	protected static RoleType getRoleTypeById(Integer id, Session session) {
		try {
			return (RoleType) roleTypeQuery
					.add( Restrictions.eq("roleTypeId", id))
					.getExecutableCriteria(session)
					.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting role type.", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected static Collection<Phone> getPhonesByUser(String userName, Session session) {
		try {
			return phoneQuery
					.add(Restrictions.eq("user.username", userName))
					.getExecutableCriteria(session)
					.list();
		} catch(Exception e) {
			throw logAndThrowError("Error getting phones.", e);
		}
	}

	@SuppressWarnings("unchecked")
	protected static Collection<Address> getAddressesByUser(String userName, Session session) {
		try {
			return addressQuery
						.add(Restrictions.eq("user.username", userName))
						.getExecutableCriteria(session)
						.list();
		} catch(Exception e) {
			throw logAndThrowError("Error getting addresses.", e);
		}
	}
	
	
	protected static User getUserByUsername(String userName, Session session) {
		try {
			return (User)userQuery.add(Restrictions.eq("username", userName))
							.getExecutableCriteria(session)
							.uniqueResult();
		} catch(Exception e){
			throw logAndThrowError("Error getting user.", e);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	protected static Collection<User> getUsers(Session session) {
		try {
			return userQuery
					.getExecutableCriteria(session)
					.list();
		} catch(Exception e){
			throw logAndThrowError("Error getting users.", e);
		}
	}
}
