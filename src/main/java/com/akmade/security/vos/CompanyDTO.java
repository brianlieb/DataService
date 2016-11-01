package com.akmade.security.vos;

public class CompanyDTO {
	public final String company;
	public final AddressDTO address;
	public final String phone;
	
	public CompanyDTO(String company, AddressDTO address, String phone) {
		this.company = company;
		this.address = address;
		this.phone = phone;
	}
}
