package com.sshtools.common.auth;

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
import java.util.Collection;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.TransportProtocolSpecification;
import com.sshtools.common.ssh2.KBIPrompt;

public class PasswordKeyboardInteractiveProvider implements
		KeyboardInteractiveProvider {

	final static int REQUESTED_PASSWORD = 1;
	final static int CHANGING_PASSWORD = 2;
	final static int FINISHED = 2;
	
	String username;
	String password;
	boolean success = false;
	String name = "password";
	String instruction = "";
	
	SshConnection con;
	PasswordAuthenticationProvider[] providers;
	int state = REQUESTED_PASSWORD;
	int maxAttempts = 2;
	PasswordAuthenticationProvider selectedProvider = null;
	
	public PasswordKeyboardInteractiveProvider() {
		
	}
	public PasswordKeyboardInteractiveProvider(
			PasswordAuthenticationProvider[] providers, SshConnection con) {
		this.providers = providers;
		this.con = con;
	}

	public boolean hasAuthenticated() {
		return success;
	}

	public boolean setResponse(String[] answers, Collection<KBIPrompt> additionalPrompts) {

		if(answers.length == 0) {
			throw new RuntimeException("Not enough answers!");
		}
		
		maxAttempts--;
		
		if(maxAttempts < 0) {
			state = FINISHED;
			return false;
		}
		
		switch(state) {
		case REQUESTED_PASSWORD:
			
			password = answers[0];
			
			try {

				for(PasswordAuthenticationProvider passwordProvider : providers) {
					selectedProvider = passwordProvider;
					success = passwordProvider.verifyPassword(con, username, password);
					if(success) {
						state = FINISHED;
						return true;
					}
				}
				
				instruction = "Sorry, try again";
				additionalPrompts.add(new KBIPrompt(getPasswordPrompt(), false));
				
				return false;
			} catch (PasswordChangeException e) {
				state = CHANGING_PASSWORD;
				maxAttempts = 2;
				
				additionalPrompts.add(new KBIPrompt(getNewPasswordPrompt(), false));
				additionalPrompts.add(new KBIPrompt(getConfirmPasswordPrompt(), false));
				
				if(e.getMessage()==null)
					instruction = getChangePasswordInstructions(username);
				else
					instruction = e.getMessage();
				
			} catch(IOException ex) {
				con.disconnect(TransportProtocolSpecification.BY_APPLICATION, ex.getMessage());
			}
			return true;
		case CHANGING_PASSWORD:
			if(answers.length < 2) {
				throw new RuntimeException("Not enough answers!");
			}
			
			String password1 = answers[0];
			String password2 = answers[1];
			
			if (password1.equals(password2)) {

				try {
					success = selectedProvider.changePassword(con,
							username, password, password1);
					if(success) {
						state = FINISHED;
						return true;
					}
				} catch (PasswordChangeException e) {	
				} catch (IOException e) {
				}

				state = CHANGING_PASSWORD;

				additionalPrompts.add(new KBIPrompt(getNewPasswordPrompt(), false));
				additionalPrompts.add(new KBIPrompt(getConfirmPasswordPrompt(), false));
				instruction = getChangePasswordFailed(username);

				return true;
		} else {
			instruction = getChangePasswordMismatch(username);
			additionalPrompts.add(new KBIPrompt(getNewPasswordPrompt(), false));
			additionalPrompts.add(new KBIPrompt(getConfirmPasswordPrompt(), false));

			return true;
		}
			
		default:
			throw new RuntimeException("We shouldn't be here");
		}
		
	}

	public KBIPrompt[] init(SshConnection con) {
		this.username = con.getUsername();
		this.con = con;
		KBIPrompt[] prompts = new KBIPrompt[1];
		prompts[0] = new KBIPrompt(getPasswordPrompt(), false);
		instruction = getInstructions(username);
		return prompts;
	}

	public String getInstruction() {
		return instruction;
	}

	public String getName() {
		return name;
	}
	
	protected String getPasswordPrompt() {
		return "Password:";
	}
	
	protected String getConfirmPasswordPrompt() {
		return "Confirm Password:";
	}
	
	protected String getNewPasswordPrompt() {
		return "New Password:";
	}
	
	protected String getInstructions(String username) {
		return "Enter password for " + username;
	}
	
	protected String getChangePasswordInstructions(String username) {
		return "Enter new password for " + username;
	}
	
	protected String getChangePasswordFailed(String username) {
		return "Password change failed! Enter new password for " + username;
	}
	
	protected String getChangePasswordMismatch(String username) {
		return "Passwords do not match! Enter new password for " + username;
	}

}
