package com.akmade.security.vos;

import java.util.Collection;

public class AccountDTO {
	public final Integer userId;
	public final String userName;
	public final String email;
	public final String firstName;
	public final Character middleInitial;
	public final String lastName;
	public final AddressDTO shippingAddress;
	public final AddressDTO mailingAddress;
	
	public final String mobilePhone;
	public final String homePhone;
	public final String workPhone;
	
	public final CompanyDTO company;
	public final Boolean primary;
	public final Boolean administrative;
	public final Boolean billing;
	public final Collection<RoleDTO> roles;
	
	public AccountDTO(Integer userId, String userName, String email, String firstName, Character middleInitial,
					String lastName, AddressDTO shippingAddress, AddressDTO mailingAddress, 
					String mobilePhone,	String homePhone, String workPhone, CompanyDTO company, 
					Boolean primary, Boolean administrative, Boolean billing,
					Collection<RoleDTO> roles) {
		this.userId = userId;
		this.userName = userName;
		this.email = email;
		this.firstName = firstName;
		this.middleInitial = middleInitial;
		this.lastName = lastName;
		this.shippingAddress = shippingAddress;
		this.mailingAddress = mailingAddress;
		this.mobilePhone = mobilePhone;
		this.homePhone = homePhone;
		this.workPhone = workPhone;
		this.company = company;
		this.primary = primary;
		this.administrative = administrative;
		this.billing = billing;
		this.roles = roles;
	}
	
}
