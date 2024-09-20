package com.sshtools.server.vsession;

/*-
 * #%L
 * Virtual Sessions
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.commands.Alias;
import com.sshtools.server.vsession.commands.Catch;
import com.sshtools.server.vsession.commands.Clear;
import com.sshtools.server.vsession.commands.Date;
import com.sshtools.server.vsession.commands.Echo;
import com.sshtools.server.vsession.commands.Env;
import com.sshtools.server.vsession.commands.Exit;
import com.sshtools.server.vsession.commands.Help;
import com.sshtools.server.vsession.commands.Input;
import com.sshtools.server.vsession.commands.ShowLastError;
import com.sshtools.server.vsession.commands.Sleep;
import com.sshtools.server.vsession.commands.Source;
import com.sshtools.server.vsession.commands.Unalias;


public class ShellCommandFactory extends CommandFactory<ShellCommand> {

	final List<CommandFactory<? extends ShellCommand>> factories = new ArrayList<CommandFactory<? extends ShellCommand>>();

	
	@SafeVarargs
	public ShellCommandFactory(CommandFactory<? extends ShellCommand>... commandFactories) {
		installShellCommands();
		factories.addAll(Arrays.asList(commandFactories));
	}
	
	
	protected void installShellCommands() {
		
//		// Builtin 
		commands.put("alias", Alias.class);
		commands.put("unalias", Unalias.class);
//		
		commands.put("exit", Exit.class);
		commands.put("echo", Echo.class);
		commands.put("input", Input.class);
		commands.put("date", Date.class);

		commands.put("env", Env.class);
		commands.put("set", Env.class);
		
		commands.put("error", ShowLastError.class);
		commands.put("help", Help.class);
		
		commands.put("msh", Msh.class);
		commands.put("source", Source.class);
		
		commands.put("clear", Clear.class);
		commands.put("sleep", Sleep.class);
		commands.put("catch", Catch.class);
		
	}
	
	public void installFactory(CommandFactory<ShellCommand> factory) {
		factories.add(factory);
	}
	
	@Override
	public java.util.Set<String> getSupportedCommands() {
		Set<String> commands = new HashSet<String>();
		commands.addAll(super.getSupportedCommands());
		for(CommandFactory<? extends ShellCommand> factory : factories) {
			commands.addAll(factory.getSupportedCommands());
		}
		return commands;
	}
	
	@Override
	protected ShellCommand newInstance(String command, SshConnection con) throws UnsupportedCommandException,
			IllegalAccessException, InstantiationException, IOException, PermissionDeniedException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		
		for(CommandFactory<? extends ShellCommand> factory : factories) {
			if(factory.supportsCommand(command)) {
				return factory.newInstance(command, con);
			}
		}
		
		return super.newInstance(command, con);
	}
	
	@Override
	public boolean supportsCommand(String command) {
		
		for(CommandFactory<? extends ShellCommand> factory : factories) {
			if(factory.supportsCommand(command)) {
				return true;
			}
		}
		
		return super.supportsCommand(command);
	}
	
	public RootShell createShell(SshConnection connection) throws PermissionDeniedException, IOException {

		RootShell shell = new RootShell(this, connection);
		configureCommand(shell, connection);
		return shell;
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
