package com.akmade.security.vos;

public class AddressDTO {
	public final String address1;
	public final String address2;
	public final String city;
	public final String state;
	public final String country;
	public final String postalCode;

	public AddressDTO (String address1, String address2, String city, String state, String country, String postalCode) {
		this.address1 = address1;
		this.address2 = address2;
		this.city = city;
		this.state = state;
		this.country = country;
		this.postalCode = postalCode;
	}
	
}
