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

import static com.akmade.security.Constants.MAILING_ADDRESS;
import static com.akmade.security.Constants.SHIPPING_ADDRESS;
import static com.akmade.security.util.RepositoryUtility.*;

import com.akmade.security.data.Address;
import com.akmade.security.data.AddressType;
import com.akmade.security.data.CompanyAddress;
import com.akmade.security.data.User;
import com.akmade.security.util.Qry;
import com.akmade.security.util.SessionUtility.CritQuery;
import com.akmade.security.util.Transaction.Txn;
import com.akmade.messaging.security.dto.SecurityDTO;

public class AddressRepo  {
	
	private static CritQuery addressTypeQuery =
			session -> session.createCriteria(AddressType.class, "addressType");
			
			
	private static CritQuery addressQuery = 
			session -> session.createCriteria(Address.class, "address")
												.createAlias("addressType", "addressType")
												.createAlias("user", "user")
												.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			
	protected static Function<AddressType, Txn> saveType =
			addressType -> prepareTransaction.apply(save).apply(addressType);
		
	protected static Function<Collection<AddressType>, Txn> saveTypes =
		addressTypes ->  prepareTransaction(save, addressTypes);

	protected static Function<AddressType, Txn> deleteType =
		addressType -> prepareTransaction.apply(delete).apply(addressType);

	protected static Function<Collection<AddressType>, Txn> deleteTypes =
			addressTypes ->  prepareTransaction(delete, addressTypes);

	protected static Function<Address, Txn> deleteAddress =
			address -> prepareTransaction.apply(delete).apply(address);

	protected static Function<Collection<Address>, Txn> deleteAddresses =
			addresses -> prepareTransaction(delete, addresses);
				
	protected static Function<Address, Txn> saveAddress =
			address -> prepareTransaction.apply(save).apply(address);

	protected static Function<Collection<Address>, Txn> saveAddresses =
			addresses -> prepareTransaction(save, addresses);

			
			
	@SuppressWarnings("unchecked")
	protected static Qry<Collection<AddressType>> getAddressTypes = 
		session -> { 
			try {
				return addressTypeQuery
							.apply(session)
							.list();
			} catch (Exception e) {
				throw logAndThrowError("Error getting address types. " + e.getMessage());
			} 
		};
		
		
	protected static Function<String, Qry<AddressType>> getAddressTypeByType =
			type ->
				session ->
				 {
					logger.info("Looking for address Type " +  type +".");
					try {
						return (AddressType) addressTypeQuery
												.apply(session)
												.add( Restrictions.eq("type", type))
												.uniqueResult();
					} catch(Exception e){
						return null;
					}
				 };
				 
	@SuppressWarnings("unchecked")
	protected static Function<String, Qry<Collection<Address>>> getAddressesByUser =
	userName ->
		session -> {
			try {
				return addressQuery
							.apply(session)
							.add(Restrictions.eq("user.username", userName))
							.list();
			} catch(Exception e) {
				throw logAndThrowError("Error getting addresses.", e);
			}
		};
		
	protected static Function<User, Function<AddressType, Qry<Address>>> getAddressByUserType =
			user ->
				type ->
					session -> {
						try {
							return (Address)addressQuery
											.apply(session)
											.add(Restrictions.eq("user.userId", user.getUserId()))
											.add(Restrictions.eq("addressType.type", type.getType()))
											.uniqueResult();
						} catch(Exception e) {
							throw logAndThrowError("Error getting address.", e);
						}
					};




	protected static Function<Collection<AddressType>, Collection<SecurityDTO.Type>> makeNewAddressTypesDTOs =
			at -> at
					.stream()
					.map(t -> SecurityDTO.Type.newBuilder()
								.setType(t.getType())
								.setDescription(t.getDescription())
								.build())
					.collect(Collectors.toList());
			
	protected static Qry<Collection<SecurityDTO.Type>> getAddressTypeDTOs =
			session -> 	makeNewAddressTypesDTOs.apply(getAddressTypes.execute(session));

	
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
					

	protected static Function<SecurityDTO.Type, Txn> persistAddressType =
			addressTypeDTO ->
				session -> {					
					AddressType addressType = getAddressTypeByType.apply(addressTypeDTO.getType()).execute(session);
					if (addressType!=null)
						saveType.apply(mutateAddressType.apply(addressType).apply(addressTypeDTO)).execute(session);
					else
						saveType.apply(makeAddressType.apply(addressTypeDTO)).execute(session);
				};
				
	protected static Function<SecurityDTO.Type, Txn> deleteAddressType =
			addressTypeDTO ->
				session -> deleteType.apply(getAddressTypeByType.apply(addressTypeDTO.getType()).execute(session)).execute(session);
				
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

	protected static Function<User, Function<SecurityDTO.Account, Qry<Collection<Address>>>> makeNewAddresses = 
		user -> 
			account -> 
				session -> {
						Set<Address> addresses = new HashSet<>();
						addresses.add(makeNewAddress.apply(user).apply(getAddressTypeByType.apply(MAILING_ADDRESS).execute(session))
								.apply(account.getMailingAddress()));
						addresses.add(makeNewAddress.apply(user).apply(getAddressTypeByType.apply(SHIPPING_ADDRESS).execute(session))
								.apply(account.getShippingAddress()));
						return addresses;
					};
	
	protected static Function<User, BiFunction<AddressType, SecurityDTO.Address, Txn>> getAddresses = 
			user -> 
				(addressType, dto) -> 
				session -> {
					Address address = getAddressByUserType.apply(user).apply(addressType).execute(session);
					if (address == null)
						saveAddress.apply(makeNewAddress.apply(user).apply(addressType).apply(dto)).execute(session);
					else
						saveAddress.apply(mutateAddress.apply(address).apply(dto)).execute(session);					
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
							deleteAddress.apply(getMailingAddress.apply(user)).execute(session);
						else 
							getAddresses.apply(user)
										.apply(getAddressTypeByType.apply(MAILING_ADDRESS).execute(session), accountDTO.getMailingAddress())
										.execute(session);
				};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistShippingAddress = 
			(user, accountDTO) -> 
				session -> {
					if (accountDTO.getShippingAddress() == null)  
						deleteAddress.apply(getShippinAddress.apply(user)).execute(session);
					else 
						getAddresses.apply(user)
									.apply(getAddressTypeByType.apply(SHIPPING_ADDRESS).execute(session), 
												accountDTO.getShippingAddress())
									.execute(session);
				};

	protected static BiFunction<User, SecurityDTO.Account, Txn> persistAddresses = 
			(user, accountDTO) -> 
				session -> persistShippingAddress.apply(user, accountDTO)
							.andThen(persistMailingAddress.apply(user, accountDTO)).execute(session);
				


}
