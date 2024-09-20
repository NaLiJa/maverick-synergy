package com.sshtools.server.vsession.commands.os;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualShellNG;
import com.sshtools.server.vsession.VirtualShellNG.WindowSizeChangeListener;

public class AbstractOSCommand extends ShellCommand {

	public AbstractOSCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}

	private PtyProcess pty;
	private Map<String, String> env;
	private File directory;
	
	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		runCommand(null, Arrays.asList(Arrays.copyOfRange(args, 1, args.length)), console);
	}
	
	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	private void runCommand(String cmd, List<String> cmdArgs,
			VirtualConsole console) throws IOException {
		
		List<String> args = configureCommand(cmd, cmdArgs, console);
		
		if (cmd == null) {
			cmd = "";
		} else {
			while (cmd.startsWith("/")) {
				cmd = cmd.substring(1);
			}
		}

		Map<String, String> penv = this.env == null ? new HashMap<String, String>(System.getenv()) : new HashMap<String, String>(this.env);
		penv.put("TERM", console.getTerminal().getType());

		var builder = new PtyProcessBuilder(args.toArray(new String[0]));
		if(directory != null)
			builder.setDirectory(directory.getAbsolutePath());
		builder.setConsole(false);
		builder.setEnvironment(penv);
		pty = builder.start();

		final InputStream in = pty.getInputStream();
		final OutputStream out = pty.getOutputStream();

		setScreenSize(console.getTerminal().getWidth(),
				console.getTerminal().getHeight());

		// Listen for window size changes
		VirtualShellNG shell = (VirtualShellNG) console.getSessionChannel();
		WindowSizeChangeListener listener = new WindowSizeChangeListener() {
			public void newSize(int rows, int cols) {
				setScreenSize(cols, rows);
			}
		};
		
		shell.addWindowSizeChangeListener(listener);

		console.getSessionChannel().pauseDataCaching();
		
		ChannelEventListener l = new ChannelEventListener() {

			@Override
			public void onChannelDataIn(Channel channel, ByteBuffer buffer) {

				byte[] tmp = new byte[buffer.remaining()];
				buffer.get(tmp);

				try {
					out.write(tmp);
					out.flush();
				} catch (IOException e) {
					Log.error("Error writing data to pty", e);
					IOUtils.closeStream(out);
					IOUtils.closeStream(in);
				}
			}
		};
		console.getSessionChannel().addEventListener(l);

		try {
			IOUtils.copy(in, console.getSessionChannel().getOutputStream());
			out.close();

			int result = pty.waitFor();
			if (result > 0) {
				throw new IOException("System command exited with error " + result);
			}
		} catch (Exception e) {
		} finally {
			try {
				console.getSessionChannel().resumeDataCaching();
			}
			finally {
				console.getSessionChannel().removeEventListener(l);
			}
		}
	}

	protected List<String> configureCommand(String cmd, List<String> cmdArgs, VirtualConsole console) throws IOException {
		
		List<String> args = new ArrayList<>();
		String shellCommand = findCommand(getName());
		if(shellCommand == null)
			throw new IOException("Cannot find command " + getName());

		args.add(shellCommand);
		if(cmdArgs!=null) {
			args.addAll(cmdArgs);
		}
		
		return args;
	}

	protected String findCommand(String command, String... places) {
		String stdbuf = execAndCapture("which", command);
		if (stdbuf == null) {
			for (String place : places) {
				File f = new File(place);
				if (f.exists()) {
					stdbuf = f.getAbsolutePath();
					break;
				}
			}
		}
		if (stdbuf != null) {
			while (stdbuf.endsWith("\n")) {
				stdbuf = stdbuf.substring(0, stdbuf.length() - 1);
			}
		}
		return stdbuf;
	}

	private void setScreenSize(int width, int height) {
		try {
			pty.setWinSize(new WinSize(width, height));
		} catch (Exception e) {
			Log.warn(String.format("Could not set new terminal size of pty to %d x %d.", width, height));

		}
	}

	private final static String execAndCapture(String... args) {
		try {
			ProcessBuilder builder = new ProcessBuilder(args);
			builder.redirectErrorStream();
			Process process = builder.start();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtils.copy(process.getInputStream(), out);
			int ret = process.waitFor();
			if (ret == 0) {
				return new String(out.toByteArray());
			}
			throw new IOException("Got non-zero return status.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
