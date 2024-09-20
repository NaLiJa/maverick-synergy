package com.sshtools.common.publickey.authorized;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import com.sshtools.common.net.CIDRNetwork;

/**
 * Implements OpenSSH patterns as defined in http://man.openbsd.org/ssh_config.5#PATTERNS
 * @author lee
 *
 */
public class Patterns {

	public static boolean matchesWithCIDR(Collection<String> patterns, String value) throws IOException {
		
		if(patterns==null || patterns.isEmpty()) {
			return false;
		}

		boolean positiveMatch = false;
		boolean negativeMatch = false;
		
		for(String pattern : patterns) {
			if(pattern.startsWith("!") && !negativeMatch) {
				if(pattern.contains("/")) {
					
					CIDRNetwork cidr = new CIDRNetwork(pattern);
					negativeMatch = cidr.isValidAddressForNetwork(value);
					continue;
				}
				if(value.matches(convertToRegex(pattern.substring(1)))) {
					negativeMatch = true;
				}
			} else if(!positiveMatch) {
				if(pattern.contains("/")) {
					
					CIDRNetwork cidr = new CIDRNetwork(pattern);
					positiveMatch = cidr.isValidAddressForNetwork(value);
					continue;
				}
				if(value.matches(convertToRegex(pattern))) {
					positiveMatch = true;
				}
			}
		}
		
		return !negativeMatch && positiveMatch;
	}
	
	public static boolean matches(Collection<String> patterns, String value) {
		
		if(patterns==null || patterns.isEmpty()) {
			return false;
		}

		boolean positiveMatch = false;
		boolean negativeMatch = false;
		
		for(String pattern : patterns) {
			if(pattern.startsWith("!") && !negativeMatch) {
				if(value.matches(convertToRegex(pattern.substring(1)))) {
					negativeMatch = true;
				}
			} else if(!positiveMatch) {
				if(value.matches(convertToRegex(pattern))) {
					positiveMatch = true;
				}
			}
		}
		
		return !negativeMatch && positiveMatch;
	}
	
	static String convertToRegex(String pattern) {
		
		pattern = pattern.replace(".", "\\.");
		pattern = pattern.replace("*", ".*");
		pattern = pattern.replace("?", ".");
		
		return pattern;
		
	}
	
	public static void main(String[] args) throws IOException {
		
		System.out.println("Expecting true: " + Patterns.matchesWithCIDR(Arrays.asList("!*.dialup.example.com", "*.example.com"), "foo.example.com"));
		System.out.println("Expecting false: " + Patterns.matchesWithCIDR(Arrays.asList("!*.dialup.example.com", "*.example.com"), "foo.dialup.example.com"));
		System.out.println("Expecting false: " + Patterns.matchesWithCIDR(Arrays.asList("!*.dialup.example.com", "*.example.com"), "foo.com"));
		
		
	}
}
