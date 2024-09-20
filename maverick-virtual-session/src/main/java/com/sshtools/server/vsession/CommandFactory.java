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
import java.util.HashMap;
import java.util.List;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public abstract class CommandFactory<T extends Command> {

	protected HashMap<String, Class<? extends T>> commands = new HashMap<String, Class<? extends T>>();
	protected List<CommandConfigurator<T>> configurators = new ArrayList<CommandConfigurator<T>>();
	
	public CommandFactory<T> addConfigurator(CommandConfigurator<T> configurator) {
		configurators.add(configurator);
		return this;
	}

	public CommandFactory<T> removeConfigurator(CommandConfigurator<T> configurator) {
		configurators.add(configurator);
		return this;
	}

	public CommandFactory<T> installCommand(String cmd, Class<? extends T> cls) {
		commands.put(cmd, cls);
		return this;
	}

	public CommandFactory<T> installCommand(Class<? extends T> cls) {
		try {
			T c = cls.getConstructor().newInstance();
			commands.put(c.getCommandName(), cls);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (SecurityException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return this;
	}

	public CommandFactory<T> uninstallCommand(String cmd) {
		commands.remove(cmd);
		return this;
	}

	public java.util.Set<String> getSupportedCommands() {
		return commands.keySet();
	}

	public boolean supportsCommand(String command) {
		return commands.containsKey(command);
	}
	
	public T createCommand(String command, SshConnection con) throws UnsupportedCommandException, IllegalAccessException, InstantiationException, IOException, PermissionDeniedException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return newInstance(command, con);
	}
	
	protected T newInstance(String command, SshConnection con) throws UnsupportedCommandException, IllegalAccessException,
			InstantiationException, IOException, PermissionDeniedException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (!commands.containsKey(command)) {
			throw new UnsupportedCommandException(command + " is not a supported command");
		}

		Class<? extends T> cls = commands.get(command);
		T c = cls.getConstructor().newInstance();
		
		configureCommand(c, con);
		
		for (CommandConfigurator<T> configurator : configurators) {
			configurator.configure(c);
		}
		return c;
	}

	protected void configureCommand(T command, SshConnection con) throws IOException, PermissionDeniedException {
		
	}

}
