/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.logging;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * Builder-style flow that drives {@link LoggingApplicationListener} initialization while
 * keeping individual steps independently testable.
 *
 * @author Codex
 */
public class LoggingInitializationFlow {

	private static final String TRACE_TEMPLATE = "LoggingInitializationFlow.%s(hash=%s)";

	private final LoggingApplicationListener listener;

	private final Log logger = LogFactory.getLog(getClass());

	private ConfigurableEnvironment environment;

	private ClassLoader classLoader;

	private LoggingSystem loggingSystem;

	private LogFile logFile;

	private LoggerGroups loggerGroups;

	public LoggingInitializationFlow(LoggingApplicationListener listener) {
		Assert.notNull(listener, "LoggingApplicationListener must not be null");
		this.listener = listener;
	}

	public LoggingInitializationFlow withEnvironment(ConfigurableEnvironment environment) {
		if (this.environment != null) {
			if (this.environment == environment || this.environment.equals(environment)) {
				logStep("withEnvironment.cached", environment);
				return this;
			}
		}
		this.environment = environment;
		logStep("withEnvironment", environment);
		return this;
	}

	public LoggingInitializationFlow withClassLoader(ClassLoader classLoader) {
		if (this.classLoader == classLoader) {
			logStep("withClassLoader.cached", classLoader);
			return this;
		}
		this.classLoader = classLoader;
		logStep("withClassLoader", classLoader);
		return this;
	}

	public LoggingInitializationFlow withLoggingSystem(LoggingSystem loggingSystem) {
		if (this.loggingSystem == loggingSystem) {
			logStep("withLoggingSystem.cached", loggingSystem);
			return this;
		}
		this.loggingSystem = loggingSystem;
		if (loggingSystem != null) {
			this.listener.setLoggingSystem(loggingSystem);
		}
		logStep("withLoggingSystem", loggingSystem);
		return this;
	}

	public LoggingInitializationFlow withLogFile(LogFile logFile) {
		if (this.logFile == logFile) {
			logStep("withLogFile.cached", logFile);
			return this;
		}
		this.logFile = logFile;
		if (logFile != null) {
			this.listener.setLogFile(logFile);
		}
		logStep("withLogFile", logFile);
		return this;
	}

	void execute() {
		logStep("execute", this.environment, this.classLoader, this.loggingSystem, this.logFile, this.loggerGroups);
		applyLoggingSystemProperties();
		initializeLogFile();
		createLoggerGroups();
		initializeEarlyLoggingLevel();
		initializeSystem();
		initializeFinalLoggingLevels();
		registerShutdownHookIfNecessary();
	}

	void applyLoggingSystemProperties() {
		logStep("applyLoggingSystemProperties", this.environment);
		if (this.environment == null) {
			return;
		}
		this.listener.applyLoggingSystemProperties(this.environment);
	}

	void initializeLogFile() {
		logStep("initializeLogFile", this.environment, this.logFile);
		if (this.environment == null) {
			return;
		}
		if (this.logFile != null) {
			this.logFile.applyToSystemProperties();
			return;
		}
		this.logFile = this.listener.initializeLogFile(this.environment);
	}

	void createLoggerGroups() {
		logStep("createLoggerGroups", this.loggerGroups);
		this.loggerGroups = this.listener.createLoggerGroups();
	}

	void initializeEarlyLoggingLevel() {
		logStep("initializeEarlyLoggingLevel", this.environment);
		if (this.environment == null) {
			return;
		}
		this.listener.initializeEarlyLoggingLevel(this.environment);
	}

	void initializeSystem() {
		logStep("initializeSystem", this.environment, this.loggingSystem, this.logFile);
		if (this.environment == null) {
			return;
		}
		LoggingSystem systemToUse = (this.loggingSystem != null) ? this.loggingSystem
				: this.listener.obtainLoggingSystem(this.classLoader);
		this.loggingSystem = systemToUse;
		this.listener.setLoggingSystem(systemToUse);
		this.listener.initializeSystem(this.environment, systemToUse, this.logFile);
	}

	void initializeFinalLoggingLevels() {
		logStep("initializeFinalLoggingLevels", this.environment, this.loggingSystem);
		if (this.environment == null || this.loggingSystem == null) {
			return;
		}
		this.listener.initializeFinalLoggingLevels(this.environment, this.loggingSystem);
	}

	void registerShutdownHookIfNecessary() {
		logStep("registerShutdownHookIfNecessary", this.environment, this.loggingSystem);
		if (this.environment == null || this.loggingSystem == null) {
			return;
		}
		this.listener.registerShutdownHookIfNecessary(this.environment, this.loggingSystem);
	}

	private void logStep(String methodName, Object... parameters) {
		if (this.logger.isTraceEnabled()) {
			String hash = Integer.toHexString(Objects.hash((Object[]) parameters));
			this.logger.trace(LogMessage.format(TRACE_TEMPLATE, methodName, hash));
		}
	}

}

