package com.akmade.security.repositories;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.akmade.security.data.Phone;
import com.akmade.security.data.PhoneType;
import com.akmade.security.data.User;
import com.akmade.security.util.Qry;
import com.akmade.security.util.SessionUtility.CritQuery;
import com.akmade.security.util.Transaction.Txn;
import com.akmade.messaging.security.dto.SecurityDTO;

import static com.akmade.security.Constants.HOME_PHONE;
import static com.akmade.security.Constants.MOBILE_PHONE;
import static com.akmade.security.Constants.WORK_PHONE;
import static com.akmade.security.util.RepositoryUtility.*;


public class PhoneRepo  {
	private static CritQuery phoneTypeQuery = 
			session -> session.createCriteria(PhoneType.class, "phoneType");
				
	private static CritQuery phoneQuery = 
			session -> session.createCriteria(Phone.class, "phone")
											.createAlias("phoneType", "phoneType")
											.createAlias("user", "user")
											.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			
	@SuppressWarnings("unchecked")
	protected static Qry<Collection<PhoneType>> getPhoneTypes =
		session -> {
			try {
				return phoneTypeQuery
						.apply(session)
						.list();
			} catch (Exception e) {
				throw logAndThrowError("Error getting phone types. " + e.getMessage());
			} 
		};
	
	protected static Function<String, Qry<PhoneType>> getPhoneTypeByType =
		type ->
			session -> {
				try {
					return (PhoneType) phoneTypeQuery
										.apply(session)
										.add( Restrictions.eq("type", type))
										.uniqueResult();
				} catch(Exception e){
					throw logAndThrowError("Error getting phone type.", e);
				}
			};
	
	@SuppressWarnings("unchecked")
	protected static Function<String, Qry<Collection<Phone>>> getPhonesByUser =
		userName ->
			session -> {
				try {
					return phoneQuery
							.apply(session)
							.add(Restrictions.eq("user.username", userName))
							.list();
				} catch(Exception e) {
					throw logAndThrowError("Error getting phones.", e);
				}
			};
	
	protected static Function<User, Function<PhoneType, Qry<Phone>>> getPhoneByUserType =
		user -> 
			type ->
				session -> {
					try {
						return (Phone)phoneQuery
										.apply(session)
										.add(Restrictions.eq("user.userId", user.getUserId()))
										.add(Restrictions.eq("phoneType.type", type.getType()))
										.uniqueResult();
					} catch(Exception e) {
						throw logAndThrowError("Error getting phone.", e);
					}
				};

	protected static Function<PhoneType, Txn> saveType =
			phoneType -> prepareTransaction.apply(save).apply(phoneType);

	protected static Function<Collection<PhoneType>, Txn> saveTypes =
			phoneTypes ->  prepareTransaction(save, phoneTypes);
			
	protected static Function<PhoneType, Txn> deleteType =
			phoneType -> prepareTransaction.apply(delete).apply(phoneType);

	protected static Function<Collection<PhoneType>, Txn> deleteTypes =
			phoneTypes ->  prepareTransaction(delete, phoneTypes);
			
	protected static Function<Phone, Txn> deletePhone =
			phone -> prepareTransaction.apply(delete).apply(phone);

	protected static Function<Collection<Phone>, Txn> deletePhones =
			phones -> prepareTransaction(delete, phones);
				
	protected static Function<Phone, Txn> savePhone =
			phone -> prepareTransaction.apply(save).apply(phone);

	protected static Function<Collection<Phone>, Txn> savePhones =
			phones -> prepareTransaction(save, phones);
	
	protected static Function<Collection<PhoneType>, Collection<SecurityDTO.Type>> makeNewPhoneTypesDTOs =
			at -> at
					.stream()
					.map(t -> SecurityDTO.Type.newBuilder()
								.setType(t.getType())
								.setDescription(t.getDescription())
								.build())
					.collect(Collectors.toList());
			
	protected static Qry<Collection<SecurityDTO.Type>> getPhoneTypeDTOS = 
			session -> makeNewPhoneTypesDTOs.apply(getPhoneTypes.execute(session));

	
	protected static Function<String, Function<Collection<Phone>, String>> makeNewPhoneDTOByType = 
			type ->
				coll -> coll.parallelStream()
							.filter(p -> p.getPhoneType().equals(type))
							.map(Phone::getPhone)
							.findFirst()
							.orElse(null);
	
	protected static Function<PhoneType, Function<SecurityDTO.Type, PhoneType>> mutatePhoneType =
			oldPhoneType ->
					dto -> {
							oldPhoneType.setType(dto.getType());
							oldPhoneType.setDescription(dto.getDescription());
							return oldPhoneType;
					};
		
	protected static Function<SecurityDTO.Type, PhoneType> makePhoneType =
			dto -> new PhoneType(dto.getType(), dto.getDescription(), null);
	
	protected static Function<SecurityDTO.Type, Txn> persistPhoneType =
			phoneTypeDTO ->
				session -> {
					PhoneType phoneType = getPhoneTypeByType.apply(phoneTypeDTO.getType()).execute(session);
					if (phoneType != null)
						saveType.apply(mutatePhoneType.apply(phoneType).apply(phoneTypeDTO)).execute(session);
					else
						saveType.apply(makePhoneType.apply(phoneTypeDTO)).execute(session);
				};
				
	protected static Function<SecurityDTO.Type, Txn> deletePhoneType =
			phoneTypeDTO ->
				session -> deleteType.apply(getPhoneTypeByType.apply(phoneTypeDTO.getType()).execute(session)).execute(session);
	
	protected static Function<User, Function<PhoneType, Function<String, Phone>>> makeNewPhone = user -> phoneType -> phoneDTO -> new Phone(
			phoneType, user, phoneDTO, new Date(), new Date());

	protected static Function<Phone, Function<String, Phone>> mutatePhone = oldPhone -> dto -> {
		oldPhone.setPhone(dto);
		oldPhone.setLastmodifiedDate(new Date());
		return oldPhone;
	};

	protected static Function<User, Function<SecurityDTO.Account, Qry<Collection<Phone>>>> makeNewPhones = user -> account -> session -> {
		Set<Phone> phones = new HashSet<>();
		phones.add(makeNewPhone.apply(user).apply(getPhoneTypeByType.apply(HOME_PHONE).execute(session))
				.apply(account.getHomePhone()));
		phones.add(makeNewPhone.apply(user).apply(getPhoneTypeByType.apply(MOBILE_PHONE).execute(session))
				.apply(account.getMobilePhone()));
		phones.add(makeNewPhone.apply(user).apply(getPhoneTypeByType.apply(WORK_PHONE).execute(session))
				.apply(account.getWorkPhone()));
		return phones;
	};
	protected static Function<User, BiFunction<PhoneType, String,Txn>> getPhones = user -> 
			(phoneType, dto) -> 
				session -> {
					Phone phone = getPhoneByUserType.apply(user).apply(phoneType).execute(session);
					if(phone == null)
						savePhone.apply(makeNewPhone.apply(user).apply(phoneType).apply(dto)).execute(session);
					else
						savePhone.apply(mutatePhone.apply(phone).apply(dto)).execute(session);
				};


	protected static Function<Collection<Phone>, Function<String, Phone>> getMyPhone = phones -> type -> phones.stream()
			.filter(p -> p.getPhoneType().getType().equals(type)).findFirst().orElse(null);

	protected static Function<User, Phone> getHomePhone = user -> getMyPhone.apply(user.getPhones()).apply(HOME_PHONE);

	protected static Function<User, Phone> getMobilePhone = user -> getMyPhone.apply(user.getPhones()).apply(MOBILE_PHONE);

	protected static Function<User, Phone> getWorkPhone = user -> getMyPhone.apply(user.getPhones()).apply(WORK_PHONE);

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistHomePhone = 
		(user, accountDTO) -> 
			session ->  {
				if (accountDTO.getHomePhone() == null)
					deletePhone.apply(getHomePhone.apply(user)).execute(session);
				else 
					getPhones.apply(user).apply(getPhoneTypeByType.apply(HOME_PHONE).execute(session), accountDTO.getHomePhone()).execute(session);
			};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistWorkPhone = 
			(user, accountDTO) -> 
				session -> {
					if (accountDTO.getWorkPhone() == null)
						deletePhone.apply(getWorkPhone.apply(user)).execute(session);
					else
						getPhones
							.apply(user)
							.apply(getPhoneTypeByType.apply(WORK_PHONE).execute(session), accountDTO.getHomePhone()).execute(session);
				};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistMobilePhone = 
		(user, accountDTO) -> 
			session -> {
				if (accountDTO.getMobilePhone() == null)
					deletePhone.apply(getMobilePhone.apply(user)).execute(session);
				else 
					getPhones.apply(user)
							.apply(getPhoneTypeByType.apply(MOBILE_PHONE).execute(session), accountDTO.getHomePhone()).execute(session);
			};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistPhones = 
			(user, accountDTO) -> 
				session -> 	persistHomePhone.apply(user, accountDTO)
								.andThen(persistWorkPhone.apply(user, accountDTO))
								.andThen(persistMobilePhone.apply(user, accountDTO)).execute(session);;

}
