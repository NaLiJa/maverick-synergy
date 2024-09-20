package com.sshtools.server.callback.commands;

/*-
 * #%L
 * Callback Server API
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
import java.util.Objects;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.tasks.ShellTask.ShellTaskBuilder;
import com.sshtools.client.tasks.Task;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualShellNG;
import com.sshtools.server.vsession.VirtualShellNG.WindowSizeChangeListener;

public class CallbackShell extends CallbackCommand {

	public CallbackShell() {
		super("shell", "Callback", "shell <uuid>", "Open a shell to the callback client identified by uuid");
	}
	
	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		
		if(args.length != 2) {
			throw new UsageException("Invalid number of arguments");
		}
		
		var clientName = args[1];
		var con = service.getCallbackByUUID(clientName);
		
		if(Objects.isNull(con)) {
			console.println(String.format("%s is not currently connected", clientName));
			return;
		}

		console.println(String.format("---- Opening shell on %s", clientName));
		console.println();
		
		var listener = new WindowSizeChange();
		
		con.addTask(ShellTaskBuilder.create().
				withConnection(con.getConnection()).
				withTermType(console.getTerminal().getType()).
				withColumns(console.getTerminal().getWidth()).
				withRows(console.getTerminal().getHeight()).
				onBeforeTask((task, session) -> {
					console.getSessionChannel().pauseDataCaching();
					listener.session = session;
					((VirtualShellNG)console.getSessionChannel()).addWindowSizeChangeListener(listener);
					con.addTask(Task.ofRunnable(con.getConnection(), (c) -> IOUtils.copy(console.getSessionChannel().getInputStream(), session.getOutputStream())));
					IOUtils.copy(session.getInputStream(), console.getSessionChannel().getOutputStream());
				}).
				onClose((task, session) -> ((VirtualShellNG)console.getSessionChannel()).removeWindowSizeChangeListener(listener)).
				build()).waitForever();
		
		console.getSessionChannel().resumeDataCaching();
		console.println();
		console.println(String.format("---- Exited shell on %s", clientName));
	}
	
	class WindowSizeChange implements WindowSizeChangeListener {
		
		SessionChannelNG session;

		@Override
		public void newSize(int rows, int cols) {
			if(session != null)
				session.changeTerminalDimensions(cols, rows, 0, 0);
		}
		
	}

}
