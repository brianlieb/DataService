package com.akmade.security.vos;

public class RoleDTO {
	public final String role;
	public final String roleType;
	public final String description;
	
	public RoleDTO(String role, String roleType, String description) {
		this.role = role;
		this.roleType = roleType;
		this.description = description;
	}
	
}
