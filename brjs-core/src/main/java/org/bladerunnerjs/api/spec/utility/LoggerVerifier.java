package org.bladerunnerjs.api.spec.utility;

import org.bladerunnerjs.api.spec.engine.SpecTest;
import org.bladerunnerjs.api.spec.engine.VerifierChainer;
import org.junit.Assert;



public class LoggerVerifier
{
	public static final String LOGGING_ENABLED_BUT_NO_ASSERTIONS_MESSAGE = "Logging was enabled, but 0 log messages were asserted. Either expect some log messages or disable logging for this test.";
	private final LogMessageStore logStore;
	private final VerifierChainer verifierChainer;
	
	public LoggerVerifier(SpecTest modelTest, LogMessageStore logStore)
	{
		this.logStore = logStore;
		verifierChainer = new VerifierChainer(modelTest);
	}

	public VerifierChainer enableLogging()
	{
		logStore.enableLogging();
		
		return verifierChainer;
	}

	public VerifierChainer disableLogging()
	{
		logStore.disableLogging();
		
		return verifierChainer;
	}

	public VerifierChainer enableStoringLogs()
	{
		if (logStore.isLoggingEnabled())
		{
			logStore.enableStoringLogs();
		}
		
		return verifierChainer;
	}
	
	public VerifierChainer disableStoringLogs()
	{
		logStore.disableStoringLogs();
		
		return verifierChainer;
	}
	
	public VerifierChainer verifyLogsReceivedIfCaptureEnabled()
	{
		if (logStore.isLoggingEnabled())
		{
			Assert.assertTrue(LOGGING_ENABLED_BUT_NO_ASSERTIONS_MESSAGE, logStore.isAssertionMade());
		}
		
		return verifierChainer;
	}
	
	public VerifierChainer noMessagesLogged()	
	{
		logStore.noMessagesLogged();
		
		return verifierChainer;
	}
	
	public VerifierChainer errorMessageReceived(String message, Object... params)
	{
		logStore.verifyErrorLogMessage(message, params);
		
		return verifierChainer;
	}

	public VerifierChainer warnMessageReceived(String message, Object... params)
	{
		logStore.verifyWarnLogMessage(message, params);
		
		return verifierChainer;
	}
	
	public VerifierChainer infoMessageReceived(String message, Object... params)
	{
		logStore.verifyInfoLogMessage(message, params);
		
		return verifierChainer;
	}

	public VerifierChainer debugMessageReceived(String message, Object... params)
	{
		logStore.verifyDebugLogMessage(message, params);
		
		return verifierChainer;
	}
	
	public VerifierChainer unorderedErrorMessageReceived(String message, Object... params)
	{
		logStore.verifyUnorderedErrorLogMessage(message, params);
		
		return verifierChainer;
	}

	public VerifierChainer unorderedWarnMessageReceived(String message, Object... params)
	{
		logStore.verifyUnorderedWarnLogMessage(message, params);
		
		return verifierChainer;
	}
	
	public VerifierChainer unorderedInfoMessageReceived(String message, Object... params)
	{
		logStore.verifyUnorderedInfoLogMessage(message, params);
		
		return verifierChainer;
	}
	
	public VerifierChainer unorderedDebugMessageReceived(String message, Object... params)
	{
		return debugMessageReceived(message, params);
	}
	
	public VerifierChainer containsFormattedConsoleMessage(String message, Object... params)
	{
		logStore.verifyFormattedConsoleLogMessage(message, params);

		return verifierChainer;
	}

	public VerifierChainer containsConsoleText(String... messages)
	{
		logStore.verifyConsoleLogMessages(messages);
		
		return verifierChainer;
	}
	
	public VerifierChainer doesNotcontainConsoleText(String message, Object... params)
	{
		logStore.verifyNoConsoleLogMessage(message, params);
		
		return verifierChainer;
	}

	public VerifierChainer verifyNoUnhandledMessages() {
		logStore.verifyNoUnhandledMessage();
		
		return verifierChainer;
	}

	public VerifierChainer otherMessagesIgnored()
	{
		logStore.clearLogs();
		
		return verifierChainer;
		
	}

	public VerifierChainer doesNotContainErrorMessage(String message, Object... params)
	{
		logStore.verifyNoErrorLogMessage(message, params);
		
		return verifierChainer;
	}
	
	public VerifierChainer doesNotContainWarnMessage(String message, Object... params)
	{
		logStore.verifyNoWarnLogMessage(message, params);
		
		return verifierChainer;
	}
	
	public VerifierChainer doesNotContainInfoMessage(String message, Object... params)
	{
		logStore.verifyNoInfoLogMessage(message, params);
		
		return verifierChainer;
	}
	
	public VerifierChainer doesNotContainDebugMessage(String message, Object... params)
	{
		logStore.verifyNoDebugLogMessage(message, params);
		
		return verifierChainer;
	}
	
}
