package com.akmade.security;

import java.util.function.Predicate;

import com.akmade.security.vos.AccountDTO;

public class SecurityBusiness {
	public static Predicate<AccountDTO> checkNewAccount = 
			acct -> true;
}
