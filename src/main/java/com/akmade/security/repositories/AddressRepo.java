package com.akmade.security.repositories;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.Session;

import com.akmade.security.data.Address;
import com.akmade.security.data.AddressType;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.User;
import com.akmade.security.dto.DataTransferObjects;

public class AddressRepo {
	
	protected static Function<Collection<AddressType>, Collection<String>> makeNewAddressTypesDTOs =
			at -> at
					.stream()
					.map(t -> new String(t.getType()))
					.collect(Collectors.toList());
			
	protected static Function<Session, Collection<String>> getAddressTypeDTOs =
			session -> 	makeNewAddressTypesDTOs.apply(QueryManager.getAddressTypes(session));

	
	protected static Function<CompanyAddress, DataTransferObjects.Address> makeNewCompanyAddressDTO =
			address -> DataTransferObjects
							.Address.newBuilder()
								.setAddress1(address.getAddress1())
								.setAddress2(address.getAddress2())
								.setCity(address.getCity())
								.setState(address.getState())
								.setCountry(address.getCountry())
								.setPostalCode(address.getPostalCode())
								.build();
	
	protected static Function<Address, DataTransferObjects.Address> makeNewAddressDTO =
			address -> DataTransferObjects
							.Address.newBuilder()
									.setAddress1(address.getAddress1())
									.setAddress2(address.getAddress2())
									.setCity(address.getCity())
									.setState(address.getState())
									.setCountry(address.getCountry())
									.setPostalCode(address.getPostalCode())
									.build();
			
	protected static Function<String, Function<Collection<Address>, DataTransferObjects.Address>> makeNewAddressDTOByType =
			type ->
				coll -> coll.parallelStream()
							.filter(a -> a.getAddressType().getType().equals(type))
							.map(a -> makeNewAddressDTO.apply(a))
							.findFirst()
							.orElse(null);

	
	protected static Function<ImmutablePair<String, String>, AddressType> makeAddressType =
			dto -> new AddressType(dto.left, dto.right, null);
			
			
	protected static Function<AddressType, Function<ImmutablePair<String, String>, AddressType>> mutateAddressType =
			oldAddressType ->
					dto -> {
								oldAddressType.setType(dto.left);
								oldAddressType.setDescription(dto.right);
								return oldAddressType;
					};
					
	protected static Function<String, Function<Session, AddressType>> getAddressTypeByType =
			type ->
				session ->
					QueryManager.getAddressTypeByType(type,session);

	protected static Function<ImmutablePair<String, String>, Function<Session, Consumer<Session>>> persistAddressType =
			addressTypeDTO ->
				session -> {					
					AddressType addressType = getAddressTypeByType.apply(addressTypeDTO.left).apply(session);
					return addressType!=null?
						CommandManager.saveAddressType.apply(mutateAddressType.apply(addressType).apply(addressTypeDTO)):
						CommandManager.saveAddressType.apply(makeAddressType.apply(addressTypeDTO));
				};
				
	protected static Function<ImmutablePair<String, String>, Function <Session, Consumer<Session>>> deleteAddressType =
			addressTypeDTO ->
				session -> CommandManager.deleteAddressType.apply(getAddressTypeByType.apply(addressTypeDTO.left).apply(session));
				
	protected static Function<User, Function<AddressType, Function<DataTransferObjects.Address, Address>>> makeNewAddress = user -> addressType -> addressDTO -> new Address(
			addressType, user, addressDTO.getAddress1(), addressDTO.getAddress2(), addressDTO.getCity(),
			addressDTO.getState(), addressDTO.getCountry(), addressDTO.getPostalCode(), new Date(), new Date());

	protected static Function<Address, Function<DataTransferObjects.Address, Address>> mutateAddress = oldAddress -> dto -> {
		oldAddress.setAddress1(dto.getAddress1());
		oldAddress.setAddress2(dto.getAddress2());
		oldAddress.setCity(dto.getCity());
		oldAddress.setState(dto.getState());
		oldAddress.setCountry(dto.getCountry());
		oldAddress.setPostalCode(dto.getPostalCode());
		oldAddress.setLastmodifiedDate(new Date());
		return oldAddress;
	};

	protected static Function<User, Function<AddressType, Function<Session, Address>>> getAddressByUserType = user -> type -> session -> QueryManager
			.getAddressByUserType(user, type, session);

	protected static Function<User, Function<DataTransferObjects.Account, Function<Session, Collection<Address>>>> makeNewAddresses = user -> account -> session -> {
		Set<Address> addresses = new HashSet<>();
		addresses.add(makeNewAddress.apply(user).apply(getAddressTypeByType.apply("Mailing").apply(session))
				.apply(account.getMailingAddress()));
		addresses.add(makeNewAddress.apply(user).apply(getAddressTypeByType.apply("Shipping").apply(session))
				.apply(account.getShippingAddress()));
		return addresses;
	};
	
	protected static Function<User, BiFunction<AddressType, DataTransferObjects.Address, Function<Session, Consumer<Session>>>> getAddresses = user -> (
			addressType, dto) -> session -> {
				Address address = getAddressByUserType.apply(user).apply(addressType).apply(session);
				return address == null
						? CommandManager.saveAddress.apply(makeNewAddress.apply(user).apply(addressType).apply(dto))
						: CommandManager.saveAddress.apply(mutateAddress.apply(address).apply(dto));
			};
			
	protected static Function<Collection<Address>, Function<String, Address>> getMyAddress = addresses -> type -> addresses
			.stream().filter(a -> a.getAddressType().getType().equals(type)).findFirst().orElse(null);
	
	protected static Function<User, Address> getMailingAddress = user -> getMyAddress.apply(user.getAddresses())
			.apply("Mailing");

	protected static Function<User, Address> getShippinAddress = user -> getMyAddress.apply(user.getAddresses())
			.apply("Shipping");

	protected static BiFunction<User, DataTransferObjects.Account, Function<Session, Consumer<Session>>> persistMailingAddress = (
			user,
			accountDTO) -> session -> accountDTO.getMailingAddress() == null
					? CommandManager.deleteAddress.apply(getMailingAddress.apply(user))
					: getAddresses.apply(user)
							.apply(getAddressTypeByType.apply("Mailing").apply(session), accountDTO.getMailingAddress())
							.apply(session);

	protected static BiFunction<User, DataTransferObjects.Account, Function<Session, Consumer<Session>>> persistShippingAddress = (
			user,
			accountDTO) -> session -> accountDTO.getShippingAddress() == null
					? CommandManager.deleteAddress.apply(getShippinAddress.apply(user))
					: getAddresses.apply(user).apply(getAddressTypeByType.apply("Shipping").apply(session),
							accountDTO.getShippingAddress()).apply(session);

	protected static BiFunction<User, DataTransferObjects.Account, Function<Session, Consumer<Session>>> persistAddresses = (
			user, accountDTO) -> session -> persistShippingAddress.apply(user, accountDTO).apply(session)
					.andThen(persistMailingAddress.apply(user, accountDTO).apply(session));



}
