package com.akmade.security;

import java.util.function.Predicate;

import com.akmade.messaging.security.dto.SecurityDTO;

public class SecurityBusiness {
	public static Predicate<SecurityDTO.Account> checkNewAccount = 
			acct -> true;
}
