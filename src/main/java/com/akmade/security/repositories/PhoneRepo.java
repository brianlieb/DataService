package com.akmade.security.repositories;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Session;

import com.akmade.security.data.Phone;
import com.akmade.security.data.PhoneType;
import com.akmade.security.data.User;
import com.akmade.messaging.security.dto.SecurityDTO;

import static com.akmade.security.Constants.HOME_PHONE;
import static com.akmade.security.Constants.MOBILE_PHONE;
import static com.akmade.security.Constants.WORK_PHONE;


public class PhoneRepo {
	protected static Function<Collection<PhoneType>, Collection<SecurityDTO.Type>> makeNewPhoneTypesDTOs =
			at -> at
					.stream()
					.map(t -> SecurityDTO.Type.newBuilder()
								.setType(t.getType())
								.setDescription(t.getDescription())
								.build())
					.collect(Collectors.toList());
			
	protected static Function<Session, Collection<SecurityDTO.Type>> getPhoneTypeDTOS = 
			session -> makeNewPhoneTypesDTOs.apply(QueryManager.getPhoneTypes(session));

	
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
						
	protected static Function<String, Function<Session, PhoneType>> getPhoneTypeByType =
			type ->
				session ->
					QueryManager.getPhoneTypeByType(type,session);
		
	protected static Function<SecurityDTO.Type, PhoneType> makePhoneType =
			dto -> new PhoneType(dto.getType(), dto.getDescription(), null);
	
	protected static Function<SecurityDTO.Type, Function<Session, Consumer<Session>>> persistPhoneType =
			phoneTypeDTO ->
				session -> {
					PhoneType phoneType = getPhoneTypeByType.apply(phoneTypeDTO.getType()).apply(session);
					return phoneType != null?
						CommandManager.savePhoneType.apply(mutatePhoneType.apply(phoneType).apply(phoneTypeDTO)):
						CommandManager.savePhoneType.apply(makePhoneType.apply(phoneTypeDTO));
				};
				
	protected static Function<SecurityDTO.Type, Function<Session, Consumer<Session>>> deletePhoneType =
			phoneTypeDTO ->
				session -> CommandManager.deletePhoneType.apply(getPhoneTypeByType.apply(phoneTypeDTO.getType()).apply(session));
	
	protected static Function<User, Function<PhoneType, Function<String, Phone>>> makeNewPhone = user -> phoneType -> phoneDTO -> new Phone(
			phoneType, user, phoneDTO, new Date(), new Date());

	protected static Function<Phone, Function<String, Phone>> mutatePhone = oldPhone -> dto -> {
		oldPhone.setPhone(dto);
		oldPhone.setLastmodifiedDate(new Date());
		return oldPhone;
	};

	protected static Function<User, Function<PhoneType, Function<Session, Phone>>> getPhoneByUserType = user -> type -> session -> QueryManager
			.getPhoneByUserType(user, type, session);

	protected static Function<User, Function<SecurityDTO.Account, Function<Session, Collection<Phone>>>> makeNewPhones = user -> account -> session -> {
		Set<Phone> phones = new HashSet<>();
		phones.add(makeNewPhone.apply(user).apply(getPhoneTypeByType.apply(HOME_PHONE).apply(session))
				.apply(account.getHomePhone()));
		phones.add(makeNewPhone.apply(user).apply(getPhoneTypeByType.apply(MOBILE_PHONE).apply(session))
				.apply(account.getMobilePhone()));
		phones.add(makeNewPhone.apply(user).apply(getPhoneTypeByType.apply(WORK_PHONE).apply(session))
				.apply(account.getWorkPhone()));
		return phones;
	};
	protected static Function<User, BiFunction<PhoneType, String, Function<Session, Consumer<Session>>>> getPhones = user -> (
			phoneType, dto) -> session -> {
				Phone phone = getPhoneByUserType.apply(user).apply(phoneType).apply(session);
				return phone == null
						? CommandManager.savePhone.apply(makeNewPhone.apply(user).apply(phoneType).apply(dto))
						: CommandManager.savePhone.apply(mutatePhone.apply(phone).apply(dto));
			};


	protected static Function<Collection<Phone>, Function<String, Phone>> getMyPhone = phones -> type -> phones.stream()
			.filter(p -> p.getPhoneType().getType().equals(type)).findFirst().orElse(null);

	protected static Function<User, Phone> getHomePhone = user -> getMyPhone.apply(user.getPhones()).apply(HOME_PHONE);

	protected static Function<User, Phone> getMobilePhone = user -> getMyPhone.apply(user.getPhones()).apply(MOBILE_PHONE);

	protected static Function<User, Phone> getWorkPhone = user -> getMyPhone.apply(user.getPhones()).apply(WORK_PHONE);

	protected static BiFunction<User, SecurityDTO.Account, Function<Session, Consumer<Session>>> persistHomePhone = (
			user,
			accountDTO) -> session -> accountDTO.getHomePhone() == null
					? CommandManager.deletePhone.apply(getHomePhone.apply(user))
					: getPhones.apply(user)
							.apply(getPhoneTypeByType.apply(HOME_PHONE).apply(session), accountDTO.getHomePhone())
							.apply(session);

	protected static BiFunction<User, SecurityDTO.Account, Function<Session, Consumer<Session>>> persistWorkPhone = (
			user,
			accountDTO) -> session -> accountDTO.getWorkPhone() == null
					? CommandManager.deletePhone.apply(getWorkPhone.apply(user))
					: getPhones.apply(user)
							.apply(getPhoneTypeByType.apply(WORK_PHONE).apply(session), accountDTO.getHomePhone())
							.apply(session);

	protected static BiFunction<User, SecurityDTO.Account, Function<Session, Consumer<Session>>> persistMobilePhone = (
			user,
			accountDTO) -> session -> accountDTO.getMobilePhone() == null
					? CommandManager.deletePhone.apply(getMobilePhone.apply(user))
					: getPhones.apply(user)
							.apply(getPhoneTypeByType.apply(MOBILE_PHONE).apply(session), accountDTO.getHomePhone())
							.apply(session);

	protected static BiFunction<User, SecurityDTO.Account, Function<Session, Consumer<Session>>> persistPhones = (
			user,
			accountDTO) -> session -> persistHomePhone.apply(user, accountDTO).apply(session)
					.andThen(persistWorkPhone.apply(user, accountDTO).apply(session))
					.andThen(persistMobilePhone.apply(user, accountDTO).apply(session));

}
