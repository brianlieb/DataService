package com.akmade.security.repositories;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Session;

import static com.akmade.security.Constants.MAILING_ADDRESS;
import static com.akmade.security.Constants.SHIPPING_ADDRESS;


import com.akmade.security.data.Address;
import com.akmade.security.data.AddressType;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.User;
import com.akmade.security.repositories.SessionRepo.Txn;
import com.akmade.messaging.security.dto.SecurityDTO;

public class AddressRepo {
	
	protected static Function<Collection<AddressType>, Collection<SecurityDTO.Type>> makeNewAddressTypesDTOs =
			at -> at
					.stream()
					.map(t -> SecurityDTO.Type.newBuilder()
								.setType(t.getType())
								.setDescription(t.getDescription())
								.build())
					.collect(Collectors.toList());
			
	protected static Function<Session, Collection<SecurityDTO.Type>> getAddressTypeDTOs =
			session -> 	makeNewAddressTypesDTOs.apply(QueryManager.getAddressTypes(session));

	
	protected static Function<CompanyAddress, SecurityDTO.Address> makeNewCompanyAddressDTO =
			address -> SecurityDTO
							.Address.newBuilder()
								.setAddress1(address.getAddress1())
								.setAddress2(address.getAddress2())
								.setCity(address.getCity())
								.setState(address.getState())
								.setCountry(address.getCountry())
								.setPostalCode(address.getPostalCode())
								.build();
	
	protected static Function<Address, SecurityDTO.Address> makeNewAddressDTO =
			address -> SecurityDTO
							.Address.newBuilder()
									.setAddress1(address.getAddress1())
									.setAddress2(address.getAddress2())
									.setCity(address.getCity())
									.setState(address.getState())
									.setCountry(address.getCountry())
									.setPostalCode(address.getPostalCode())
									.build();
			
	protected static Function<String, Function<Collection<Address>, SecurityDTO.Address>> makeNewAddressDTOByType =
			type ->
				coll -> coll.parallelStream()
							.filter(a -> a.getAddressType().getType().equals(type))
							.map(a -> makeNewAddressDTO.apply(a))
							.findFirst()
							.orElse(null);

	
	protected static Function<SecurityDTO.Type, AddressType> makeAddressType =
			dto -> new AddressType(dto.getType(), dto.getDescription(), null);
			
			
	protected static Function<AddressType, Function<SecurityDTO.Type, AddressType>> mutateAddressType =
			oldAddressType ->
					dto -> {
								oldAddressType.setType(dto.getType());
								oldAddressType.setDescription(dto.getDescription());
								return oldAddressType;
					};
					
	protected static Function<String, Function<Session, AddressType>> getAddressTypeByType =
			type ->
				session ->
					QueryManager.getAddressTypeByType(type,session);

	protected static Function<SecurityDTO.Type, Txn> persistAddressType =
			addressTypeDTO ->
				session -> {					
					AddressType addressType = getAddressTypeByType.apply(addressTypeDTO.getType()).apply(session);
					if (addressType!=null)
						CommandManager.saveAddressType.apply(mutateAddressType.apply(addressType).apply(addressTypeDTO)).accept(session);
					else
						CommandManager.saveAddressType.apply(makeAddressType.apply(addressTypeDTO)).accept(session);
				};
				
	protected static Function<SecurityDTO.Type, Txn> deleteAddressType =
			addressTypeDTO ->
				session -> CommandManager.deleteAddressType.apply(getAddressTypeByType.apply(addressTypeDTO.getType()).apply(session)).accept(session);;
				
	protected static Function<User, Function<AddressType, Function<SecurityDTO.Address, Address>>> makeNewAddress = 
			user -> 
				addressType -> 
					addressDTO -> new Address(addressType, 
												user, 
												addressDTO.getAddress1(), 
												addressDTO.getAddress2(), 
												addressDTO.getCity(),
												addressDTO.getState(), 
												addressDTO.getCountry(), 
												addressDTO.getPostalCode(), 
												new Date(), 
												new Date());

	protected static Function<Address, Function<SecurityDTO.Address, Address>> mutateAddress = 
			oldAddress -> 
				dto -> {
						oldAddress.setAddress1(dto.getAddress1());
						oldAddress.setAddress2(dto.getAddress2());
						oldAddress.setCity(dto.getCity());
						oldAddress.setState(dto.getState());
						oldAddress.setCountry(dto.getCountry());
						oldAddress.setPostalCode(dto.getPostalCode());
						oldAddress.setLastmodifiedDate(new Date());
						return oldAddress;
				};

	protected static Function<User, Function<AddressType, Function<Session, Address>>> getAddressByUserType = 
			user -> 
				type -> 
					session -> QueryManager.getAddressByUserType(user, type, session);

	protected static Function<User, Function<SecurityDTO.Account, Function<Session, Collection<Address>>>> makeNewAddresses = 
		user -> 
			account -> 
				session -> {
						Set<Address> addresses = new HashSet<>();
						addresses.add(makeNewAddress.apply(user).apply(getAddressTypeByType.apply(MAILING_ADDRESS).apply(session))
								.apply(account.getMailingAddress()));
						addresses.add(makeNewAddress.apply(user).apply(getAddressTypeByType.apply(SHIPPING_ADDRESS).apply(session))
								.apply(account.getShippingAddress()));
						return addresses;
					};
	
	protected static Function<User, BiFunction<AddressType, SecurityDTO.Address, Txn>> getAddresses = 
			user -> 
				(addressType, dto) -> 
				session -> {
					Address address = getAddressByUserType.apply(user).apply(addressType).apply(session);
					if (address == null)
						CommandManager.saveAddress.apply(makeNewAddress.apply(user).apply(addressType).apply(dto)).accept(session);
					else
						CommandManager.saveAddress.apply(mutateAddress.apply(address).apply(dto)).accept(session);					
					};
			
	protected static Function<Collection<Address>, Function<String, Address>> getMyAddress = addresses -> type -> addresses
			.stream().filter(a -> a.getAddressType().getType().equals(type)).findFirst().orElse(null);
	
	protected static Function<User, Address> getMailingAddress = user -> getMyAddress.apply(user.getAddresses())
			.apply(MAILING_ADDRESS);

	protected static Function<User, Address> getShippinAddress = user -> getMyAddress.apply(user.getAddresses())
			.apply(SHIPPING_ADDRESS);

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistMailingAddress = 
			(user, accountDTO) -> 
				session -> {
						if (accountDTO.getMailingAddress() == null) 
							CommandManager.deleteAddress.apply(getMailingAddress.apply(user)).accept(session);
						else 
							getAddresses.apply(user)
										.apply(getAddressTypeByType.apply(MAILING_ADDRESS).apply(session), accountDTO.getMailingAddress())
										.accept(session);
				};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistShippingAddress = 
			(user, accountDTO) -> 
				session -> {
					if (accountDTO.getShippingAddress() == null)  
						CommandManager.deleteAddress.apply(getShippinAddress.apply(user)).accept(session);
					else 
						getAddresses.apply(user)
									.apply(getAddressTypeByType.apply(SHIPPING_ADDRESS).apply(session), 
												accountDTO.getShippingAddress())
									.accept(session);
				};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistAddresses = 
			(user, accountDTO) -> 
				session -> persistShippingAddress.apply(user, accountDTO)
							.andThen(persistMailingAddress.apply(user, accountDTO)).accept(session);



}
