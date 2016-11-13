package com.akmade.security;

import java.util.function.Predicate;

import com.akmade.security.dto.DataTransferObjects;

public class SecurityBusiness {
	public static Predicate<DataTransferObjects.Account> checkNewAccount = 
			acct -> true;
}
