package com.sshtools.common.logger;

/*-
 * #%L
 * Logging API
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

public class Log {

	
	static RootLoggerContext defaultContext = null;
	static ThreadLocal<LoggerContext> currentContext = new ThreadLocal<LoggerContext>();
	
	public static RootLoggerContext getDefaultContext() {
		synchronized(Log.class) {
			if(defaultContext==null) {
				try {
				defaultContext = new DefaultLoggerContext();
				} catch(IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
			return defaultContext;
		}
	}
	
	public void shutdown() {
		defaultContext.shutdown();
	}
	
	public static void setDefaultContext(RootLoggerContext loggerContext) {
		synchronized(Log.class) {
			defaultContext = loggerContext;
		}
	}
	
	public static void enableConsole(Level level) {
		getDefaultContext().enableConsole(level);
	}
	
	public enum Level {
		NONE,
		ERROR,
		WARN,
		INFO,
		DEBUG,
		TRACE
	}
	
	public static void setupCurrentContext(LoggerContext context) {
		currentContext.set(context);
	}
	
	public static void clearCurrentContext() {
		currentContext.remove();
	}
	
	public static boolean isWarnEnabled() {
		return isLevelEnabled(Level.WARN);
	}

	public static boolean isErrorEnabled() {
		return isLevelEnabled(Level.ERROR);
	}

	public static boolean isInfoEnabled() {
		return isLevelEnabled(Level.INFO);
	}

	public static boolean isDebugEnabled() {
		return isLevelEnabled(Level.DEBUG);
	}

	public static boolean isTraceEnabled() {
		return isLevelEnabled(Level.TRACE);
	}

	public static boolean isLevelEnabled(Level level) {
		LoggerContext ctx = currentContext.get();
		if((!Objects.isNull(ctx) && ctx.isLogging(level))) {
			return ctx.isLogging(level);
		} else {
			return Log.getDefaultContext().isLogging(level);
		}
	}
	
	public static void info(String msg, Object... args) {
		log(Level.INFO, msg, null, args);
	}
	
	public static void info(String msg, Throwable e, Object... args) {
		log(Level.INFO, msg, e, args);
	}

	public static void debug(String msg, Object... args) {
		log(Level.DEBUG, msg, null, args);
	}
	
	public static void debug(String msg, Throwable e, Object... args) {
		log(Level.DEBUG, msg, e, args);
	}

	public static void trace(String msg, Object... args) {
		log(Level.TRACE, msg, null, args);
	}
	
	public static void trace(String msg, Throwable e, Object... args) {
		log(Level.TRACE, msg, e, args);
	}

	public static void error(String msg, Throwable e, Object... args) {
		log(Level.ERROR, msg, e, args);
	}

	public static void warn(String msg, Throwable e, Object... args) {
		log(Level.WARN, msg, e, args);
	}

	public static void warn(String msg, Object... args) {
		log(Level.WARN, msg, null, args);
	}

	public static void error(String msg, Object... args) {
		log(Level.ERROR, msg, null, args);
	}

	protected static void log(Level level, String msg, Throwable e, Object... args) {
		
		LoggerContext ctx = currentContext.get();
		if(!Objects.isNull(ctx) && ctx.isLogging(level)) {
			contextLog(ctx, level, msg, e, args);
		} else {
			contextLog(Log.getDefaultContext(), level, msg, e, args);
		}
	}
	
	private static void contextLog(LoggerContext ctx, Level level, String msg, Throwable e, Object... args) {
		ctx.log(level, msg, e, args);
	}

	public static void raw(Level level, String msg, boolean newline) {
		LoggerContext ctx = currentContext.get();
		if(!Objects.isNull(ctx) && ctx.isLogging(level)) {
			ctx.raw(level, msg);
			if(newline) {
				ctx.newline();
			}
		} else {
			Log.getDefaultContext().raw(level, msg);
			if(newline) {
				Log.getDefaultContext().newline();
			}
		}
	}
}
