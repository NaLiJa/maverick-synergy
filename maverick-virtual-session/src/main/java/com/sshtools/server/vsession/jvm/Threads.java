package com.sshtools.server.vsession.jvm;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Threads extends ShellCommand {
	public Threads() {
		super("threads", SUBSYSTEM_JVM, UsageHelper.build("threads <threadId>..",
				"-1     Set specified thread(s) priority to MINIMUM",
				"-2     Set specified thread(s) priority to NORMAL",
				"-3     Set specified thread(s) priority to MAXIMUM"),"List all running threads");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {

		List<?> argList = new ArrayList<>(Arrays.asList(args));
		argList.remove(0);
		if (argList.size() == 0) {
			listAllThreads(console);
		} else {
			ThreadGroup root = getRootThread();
			Thread[] threadArray = new Thread[2000];
			int threads = root.enumerate(threadArray);
			for (int i = 0; i < threads; i++) {
				for (Iterator<?> it = argList.iterator(); it.hasNext();) {
					long id = Long.parseLong((String) it.next());
					Thread thread = threadArray[i];
					if (thread.getId() == id) {
						if (CliHelper.hasShortOption(args, '1')) {
							thread.setPriority(Thread.MIN_PRIORITY);
						} else if (CliHelper.hasShortOption(args, '2')) {
							thread.setPriority(Thread.NORM_PRIORITY);
						} else if (CliHelper.hasShortOption(args, '3')) {
							thread.setPriority(Thread.MAX_PRIORITY);
						} else {
							printThreadInfo(console, thread, "");
							for (StackTraceElement el : thread.getStackTrace()) {
								console.println(el.toString());
							}
						}
					}
				}
			}
		}
	}

	private static void printThreadInfo(VirtualConsole console, Thread t, String indent) throws IOException {
		if (t == null)
			return;
		console.println(indent + "Thread: " + (String.format("%8d", t.getId())) + " " + t.getName() + "  Priority: "
			+ t.getPriority() + (t.isDaemon() ? " Daemon" : "") + (t.isAlive() ? "" : " Not Alive"));
	}

	private static void printGroupInfo(VirtualConsole console, ThreadGroup g, String indent) throws IOException {
		if (g == null)
			return;
		int numThreads = g.activeCount();
		int numGroups = g.activeGroupCount();
		Thread[] threads = new Thread[numThreads];
		ThreadGroup[] groups = new ThreadGroup[numGroups];

		g.enumerate(threads, false);
		g.enumerate(groups, false);

		console.println(indent + "Thread Group: " + g.getName() + "  Max Priority: " + g.getMaxPriority()
			+ (g.isDaemon() ? " Daemon" : ""));

		for (int i = 0; i < numThreads; i++)
			printThreadInfo(console, threads[i], indent + "    ");
		for (int i = 0; i < numGroups; i++)
			printGroupInfo(console, groups[i], indent + "    ");
	}

	public static void listAllThreads(VirtualConsole console) throws IOException {
		ThreadGroup rootThreadGroup = getRootThread();
		printGroupInfo(console, rootThreadGroup, "");
	}

	protected static ThreadGroup getRootThread() {

		ThreadGroup currentThreadGroup = Thread.currentThread().getThreadGroup();

		ThreadGroup rootThreadGroup = currentThreadGroup;
		ThreadGroup parent;
		parent = rootThreadGroup.getParent();
		while (parent != null) {
			rootThreadGroup = parent;
			parent = parent.getParent();
		}
		return rootThreadGroup;
	}
}
