package org.bladerunnerjs.spec.command;

import static org.bladerunnerjs.plugin.commands.standard.BuildAppCommand.Messages.*;

import java.io.File;

import org.bladerunnerjs.api.App;
import org.bladerunnerjs.api.Aspect;
import org.bladerunnerjs.api.Blade;
import org.bladerunnerjs.api.Bladeset;
import org.bladerunnerjs.api.model.exception.command.ArgumentParsingException;
import org.bladerunnerjs.api.model.exception.command.CommandArgumentsException;
import org.bladerunnerjs.api.model.exception.command.DirectoryDoesNotExistCommandException;
import org.bladerunnerjs.api.model.exception.command.DirectoryNotEmptyCommandException;
import org.bladerunnerjs.api.model.exception.command.NodeDoesNotExistException;
import org.bladerunnerjs.api.spec.engine.SpecTest;
import org.bladerunnerjs.appserver.util.TokenReplacementException;
import org.bladerunnerjs.utility.AppMetadataUtility;
import org.bladerunnerjs.utility.LoggingMissingTokenHandler;
import org.junit.Before;
import org.junit.Test;


public class BuildAppCommandTest extends SpecTest
{

	App app;
	Aspect defaultAspect;
	Bladeset bladeset;
	Blade blade;
	Blade badBlade;
	private App otherApp;

	private StringBuffer filePath = new StringBuffer();
	
	@Before
	public void initTestObjects() throws Exception
	{
		given(brjs).hasBeenAuthenticallyCreated();
		createModelObjects();
	}
	
	private void createModelObjects() {
		app = brjs.app("app");
		defaultAspect = app.defaultAspect();
		otherApp = brjs.app("other-app");
		bladeset = app.bladeset("bladeset");
		blade = bladeset.blade("blade");
		badBlade = bladeset.blade("!$%$^");
	}

	private void recreateBrjsWithMockVersionGenerator() throws Exception
	{
		brjs.close();
		brjs = null;
		given(brjs).automaticallyFindsAllPlugins()
			.and(brjs).hasBeenCreated();
		createModelObjects();
	}
	
	@Test
	public void exceptionIsThrownIfThereAreTooFewArguments() throws Exception
	{
		when(brjs).runCommand("build-app");
		then(exceptions).verifyException(ArgumentParsingException.class, unquoted("Parameter 'app-name' is required"))
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}

	@Test
	public void exceptionIsThrownIfTheAppDoesntExist() throws Exception
	{
		when(brjs).runCommand("build-app", "app");
		then(exceptions).verifyException(NodeDoesNotExistException.class, "app", unquoted(app.getClass().getSimpleName()))
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}

	@Test
	public void exceptionIsThrownIfAnInvalidTargetDirIsProvided() throws Exception
	{
		given(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app", "target");
		then(exceptions).verifyException(DirectoryDoesNotExistCommandException.class, "target")
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}

	@Test
	public void appIsExportedToGeneratedExportedAppsDirByDefault() throws Exception
	{
		given(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app");
		then(brjs).hasDir("generated/built-apps/app")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("generated/built-apps/app").getAbsolutePath());
	}

	@Test
	public void appOverwritesExistingBuiltAppIfBuildingToTheDefaultLocation() throws Exception
	{
		given(app).hasBeenCreated().and(brjs).commandHasBeenRun("build-app", "app");
		when(brjs).runCommand("build-app", "app");
		then(brjs).hasDir("generated/built-apps/app")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("generated/built-apps/app").getAbsolutePath())
			.and(exceptions).verifyNoOutstandingExceptions();
	}

	@Test
	public void appOverwritesExistingWarIfBuildingToTheDefaultLocation() throws Exception
	{
		given(app).hasBeenCreated().and(brjs).commandHasBeenRun("build-app", "app", "-w");
		when(brjs).runCommand("build-app", "app", "-w");
		then(brjs).hasFile("generated/built-apps/app.war")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("generated/built-apps/app.war").getAbsolutePath())
			.and(exceptions).verifyNoOutstandingExceptions();
	}

	@Test
	public void buildingAWarShouldNotDeleteAPreviousBuildStaticApp() throws Exception
	{
		given(app).hasBeenCreated().and(brjs).commandHasBeenRun("build-app", "app");
		when(brjs).runCommand("build-app", "app", "-w");
		then(brjs).hasDir("generated/built-apps/app")
			.and(brjs).hasFile("generated/built-apps/app.war")
			.and(exceptions).verifyNoOutstandingExceptions();
	}

	@Test
	public void buildingAStaticAppShouldNotDeleteAPreviouslyBuiltWar() throws Exception
	{
		given(app).hasBeenCreated().and(brjs).commandHasBeenRun("build-app", "app", "-w");
		when(brjs).runCommand("build-app", "app");
		then(brjs).hasDir("generated/built-apps/app")
			.and(brjs).hasFile("generated/built-apps/app.war")
			.and(exceptions).verifyNoOutstandingExceptions();
	}

	@Test
	public void overwrittenAppsInDefaultLocationDontNukeOtherBuiltApps() throws Exception
	{
		given(app).hasBeenCreated().and(otherApp).hasBeenCreated().and(brjs).commandHasBeenRun("build-app", "other-app").and(brjs).commandHasBeenRun("build-app", "app");
		when(brjs).runCommand("build-app", "app");
		then(brjs).hasDir("generated/built-apps/other-app")
			.and(brjs).hasDir("generated/built-apps/app");
	}

	@Test
	public void appCanBeExportedToASpecifiedDirectory() throws Exception
	{
		given(app).hasBeenCreated().and(brjs).hasDir("sdk/target");
		when(brjs).runCommand("build-app", "app", "target");
		then(brjs).hasDir("sdk/target")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("sdk/target").getAbsolutePath());
	}

	@Test
	public void appDoesntOverwriteExistingBuiltAppIfBuildingToACustomLocation() throws Exception
	{
		given(app).hasBeenCreated()
			.and(app.defaultAspect()).indexPageHasContent("index page")
			.and(brjs).localeSwitcherHasContents("locale-forwarder.js")
			.and(brjs).hasDir("sdk/target")
			.and(brjs).commandHasBeenRun("build-app", "app", "target");
		when(brjs).runCommand("build-app", "app", "target");
		then(exceptions).verifyException(DirectoryNotEmptyCommandException.class, brjs.file("sdk/target").getAbsolutePath())
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}

	@Test
	public void appCanBeExportedToASpecifiedAbsoluteDirectory() throws Exception
	{
		given(app).hasBeenCreated().and(brjs).hasDir("sdk/target");
		when(brjs).runCommand("build-app", "app", brjs.file("sdk/target").getAbsolutePath());
		then(brjs).hasDir("sdk/target")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("sdk/target").getAbsolutePath());
	}

	@Test
	public void appCanBeExportedAsAWar() throws Exception
	{
		given(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app", "-w");
		then(brjs).doesNotHaveDir("sdk/app").and(brjs).hasFile("generated/built-apps/app.war")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("generated/built-apps/app.war").getAbsolutePath());
	}
	
	@Test
	public void defaultProdPropertyIsNotApendedToWarName() throws Exception
	{
		given(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app", "-w");
		then(brjs).doesNotHaveDir("sdk/app")
			.and(brjs).hasFile("generated/built-apps/app.war")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("generated/built-apps/app.war").getAbsolutePath());
	}
	
	@Test
	public void defaultProdPropertyIsNotApendedToStaticAppDirName() throws Exception
	{
		given(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app");
		then(brjs).doesNotHaveDir("sdk/app")
			.and(brjs).hasDir("generated/built-apps/app");
	}
	
	@Test
	public void propertyIsApendedToWarNameIfSetViaCommandLine() throws Exception
	{
		given(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app", "-w", "-e", "prod");
		then(brjs).doesNotHaveDir("sdk/app")
			.and(brjs).hasFile("generated/built-apps/app_prod.war")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("generated/built-apps/app_prod.war").getAbsolutePath());
	}
	
	@Test
	public void propertyIsApendedToStaticAppDirNameIfSetViaCommandLine() throws Exception
	{
		given(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app", "-e", "prod");
		then(brjs).doesNotHaveDir("sdk/app")
			.and(brjs).hasDir("generated/built-apps/app_prod");
	}

	@Test
	public void appWithThemedDefaultAspectCanBeExportedAsAWar() throws Exception
	{
		given(brjs).usesProductionTemplates()
			.and(brjs.appJars()).containsFile("some-jar.jar")
			.and(brjs).commandHasBeenRun("create-app", "app")
			.and(defaultAspect).containsFileWithContents("themes/standard/style.css", "ASPECT theme content")
			.and(brjs).localeSwitcherHasContents("locale-forwarder.js");
		when(brjs).runCommand("build-app", "app", "-w");
		then(brjs).doesNotHaveDir("sdk/app")
			.and(brjs).hasFile("generated/built-apps/app.war")
			.and(logging).containsFormattedConsoleMessage(APP_BUILT_CONSOLE_MSG, "app", brjs.file("generated/built-apps/app.war").getAbsolutePath());
	}

	@Test
	public void webXmlDevEnvironmentIsFiltered_StaticExport() throws Exception
	{
		given(app).hasBeenCreated().and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml><!-- start-env: dev --><dev-config /><!-- end-env --></web-xml>");
		when(brjs).runCommand("build-app", "app");
		then(brjs).fileContentsDoesNotContain("generated/built-apps/app/WEB-INF/web.xml", "dev-config")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app/WEB-INF/web.xml", "start-env")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app/WEB-INF/web.xml", "end-env");
	}

	@Test
	public void webXmlDevEnvironmentIsFiltered_WarExport() throws Exception
	{
		given(app).hasBeenCreated().and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml><!-- start-env: dev --><dev-config /><!-- end-env --></web-xml>");
		when(brjs).runCommand("build-app", "app", "-w").and(brjs).zipFileIsExtractedTo("generated/built-apps/app.war", "generated/built-apps/app.war.exploded");
		then(brjs).fileContentsDoesNotContain("generated/built-apps/app.war.exploded/WEB-INF/web.xml", "dev-config")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app.war.exploded/WEB-INF/web.xml", "start-env")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app.war.exploded/WEB-INF/web.xml", "end-env");
	}

	@Test
	public void webXmlProdEnvironmentIsEnabled_StaticExport() throws Exception
	{
		given(app).hasBeenCreated().and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml><!-- start-env: prod\n" + "<prod-config />\n" + "end-env --></web-xml>");
		when(brjs).runCommand("build-app", "app");
		then(brjs).fileContentsContains("generated/built-apps/app/WEB-INF/web.xml", "<prod-config")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app/WEB-INF/web.xml", "start-env")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app/WEB-INF/web.xml", "end-env");
	}

	@Test
	public void webXmlProdEnvironmentIsEnabled_WarExport() throws Exception
	{
		given(app).hasBeenCreated().and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml><!-- start-env: prod\n" + "<prod-config />\n" + "end-env --></web-xml>");
		when(brjs).runCommand("build-app", "app", "-w").and(brjs).zipFileIsExtractedTo("generated/built-apps/app.war", "generated/built-apps/app.war.exploded");
		then(brjs).fileContentsContains("generated/built-apps/app.war.exploded/WEB-INF/web.xml", "<prod-config")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app.war.exploded/WEB-INF/web.xml", "start-env")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app.war.exploded/WEB-INF/web.xml", "end-env");
	}
	
	@Test
	public void webXmlNameSpaceIsPreserved_WarExport() throws Exception
	{
		given(app).hasBeenCreated().and(app).containsFileWithContents("WEB-INF/web.xml", 
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
					"<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
					"version=\"2.5\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\">" +
				"</web-app>");
		when(brjs).runCommand("build-app", "app", "-w").and(brjs).zipFileIsExtractedTo("generated/built-apps/app.war", "generated/built-apps/app.war.exploded");
		then(brjs).fileContentsContains("generated/built-apps/app.war.exploded/WEB-INF/web.xml", "xmlns=\"http://java.sun.com/xml/ns/javaee\"");
	}

	@Test
	public void legacyAppVersionTokenIsReplacedInWebXml() throws Exception
	{
		given(app).hasBeenCreated()
			.and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml>@appVersion@</web-xml>")
			.and(logging).enabled();
		when(brjs).runCommand("build-app", "app", "-v", "1234");
		then(brjs).fileContentsContains("generated/built-apps/app/WEB-INF/web.xml", "<web-xml>1234")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app/WEB-INF/web.xml", "@appVersion@")
			.and(logging).warnMessageReceived(AppMetadataUtility.DEPRECATED_TOKEN_WARNING, "@appVersion@", "@BRJS.APP.VERSION@");
	}
	
	@Test
	public void staticBRJSAppTokensAreReplacedInWebXml() throws Exception
	{
		recreateBrjsWithMockVersionGenerator();
		given(app).hasBeenCreated()
			.and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml>@BRJS.APP.VERSION@ @BRJS.APP.NAME@</web-xml>");
		when(brjs).runCommand("build-app", "app", "-v", "1234");
		then(brjs).fileContentsContains("generated/built-apps/app/WEB-INF/web.xml", "<web-xml>1234 app")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app/WEB-INF/web.xml", "@BRJS.APP.VERSION@")
			.and(brjs).fileContentsDoesNotContain("generated/built-apps/app/WEB-INF/web.xml", "@BRJS.APP.NAME@");
	}
	
	@Test
	public void staticAppTokensAreReplacedInWebXml() throws Exception
	{
		given(app).hasBeenCreated()
			.and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml>@MY.TOKEN@</web-xml>")
			.and(app).hasDefaultEnvironmentProperties("MY.TOKEN", "token replacement");
		when(brjs).runCommand("build-app", "app", "-v", "1234");
		then(brjs).fileContentsContains("generated/built-apps/app/WEB-INF/web.xml", "<web-xml>token replacement");
	}
	
	@Test
	public void staticAppTokensUsedTheCorrectEnvironmentAreReplacedInWebXml() throws Exception
	{
		given(app).hasBeenCreated()
			.and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml>@MY.TOKEN@</web-xml>")
			.and(app).hasDefaultEnvironmentProperties("MY.TOKEN", "token replacement")
			.and(app).hasDefaultEnvironmentProperties("MY.TOKEN", "prod replacement");
		when(brjs).runCommand("build-app", "app", "-v", "1234", "-e", "myprod");
		then(brjs).fileContentsContains("generated/built-apps/app_myprod/WEB-INF/web.xml", "<web-xml>prod replacement");
	}
	
	/*
	 * this test should fail when we remove the underscore from the getVersionPattern regex, but doesn't. needs investigation
	 */
	@Test
	public void appVersiontWithUnderscores() throws Exception
	{
		given(app).hasBeenCreated()
			.and(app).containsFileWithContents("WEB-INF/web.xml", "<web-xml>@appVersion@</web-xml>");
		when(brjs).runCommand("build-app", "app", "-v", "1.2.3_BOB");
		then(brjs).fileContentsContains("generated/built-apps/app/WEB-INF/web.xml", "<web-xml>1.2.3_BOB");
	}

	@Test
	public void exceptionIsThrownWhenVersionInWrongFormat() throws Exception
	{
		given(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app", "-v", "1.2.3 BOB");
		then(exceptions).verifyException(IllegalArgumentException.class, "([a-zA-Z0-9\\._\\-]+)");
	}

	@Test
	public void commandIsAutomaticallyLoaded() throws Exception
	{
		given(brjs).hasBeenAuthenticallyCreated().and(app).hasBeenCreated();
		when(brjs).runCommand("build-app", "app");
		then(exceptions).verifyNoOutstandingExceptions();
	}
	
	@Test
	public void defaultAspectsAreBuiltCorrectlyForSingleLocaleApps() throws Exception
	{
		given(app).hasBeenCreated()
			.and(brjs).localeSwitcherHasContents("")
			.and(app.defaultAspect()).hasBeenCreated()
			.and(app.appConf()).supportsLocales("en_GB")
			.and(app.defaultAspect()).indexPageHasContent("DEFAULT ASPECT INDEX PAGE");
		when(brjs).runCommand("build-app", "app");
		then(brjs).fileContentsContains("generated/built-apps/app/index.html", "DEFAULT ASPECT INDEX PAGE");
	}
	
	@Test
	public void defaultAspectsAreBuiltCorrectlyForMultiLocaleApps() throws Exception
	{
		given(app).hasBeenCreated()
			.and(brjs).localeSwitcherHasContents("")
			.and(app.defaultAspect()).hasBeenCreated()
			.and(app.appConf()).supportsLocales("en", "en_GB")
			.and(app.defaultAspect()).indexPageHasContent("DEFAULT ASPECT INDEX PAGE");
		when(brjs).runCommand("build-app", "app");
		then(brjs).fileContentsContains("generated/built-apps/app/en_GB.html", "DEFAULT ASPECT INDEX PAGE");
	}
	
	@Test
	public void versionIsConfigurable() throws Exception
	{
		brjs = null;
		given(brjs).hasBeenAuthenticallyReCreated()
			.and(brjs).localeSwitcherHasContents("")
			.and(brjs).usedForServletModel();
			App app = brjs.app("app1");
    		Aspect aspect = app.defaultAspect();
    		given(aspect).hasClass("appns/Class1")
			.and(aspect).hasClass("appns/Class2")
			.and(aspect).indexPageHasContent("<@js.bundle@/>\nrequire('appns/Class1');");
		when(brjs).runCommand("build-app", "app1", "-v", "myversion");
		then(brjs).hasDirectoryWithFormat("generated/built-apps/app1/v/", "myversion\\-.*", filePath)
			.and(new File(filePath.toString())).containsFileWithContents("/js/prod/combined/bundle.js", "module.exports.APP_VERSION = '"+new File(filePath.toString()).getName()+"';");
	}
	
	@Test
	public void tokensCanBeReplacedFromDefaultEnvironmentPropertiesFile() throws Exception
	{
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(app).hasDefaultEnvironmentProperties("SOME.TOKEN", "token replacement")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).indexPageHasContent("@SOME.TOKEN@");
		when(brjs).runCommand("build-app", "app");
		then(brjs).fileContentsContains("generated/built-apps/app/index.html", "token replacement");
	}

	@Test
	public void tokensFromPropertiesFilesCanBeReplacedInBundles() throws Exception
	{
		recreateBrjsWithMockVersionGenerator();
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(app).hasDefaultEnvironmentProperties("SOME.TOKEN", "token replacement")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123");
		when(brjs).runCommand("build-app", "app");
		then(brjs).fileContentsContains("generated/built-apps/app/v/123/js/prod/combined/bundle.js", "token replacement");
	}
	
	@Test
	public void prodIsTheDefaultEnvironmentIfNoneIsSpecified() throws Exception
	{
		recreateBrjsWithMockVersionGenerator();
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(app).hasDefaultEnvironmentProperties("SOME.TOKEN", "token replacement")
				.and(app).hasEnvironmentProperties("prod", "SOME.TOKEN", "prod replacement")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123");
		when(brjs).runCommand("build-app", "app");
		then(brjs).fileContentsContains("generated/built-apps/app/v/123/js/prod/combined/bundle.js", "prod replacement");
	}

	@Test
	public void environmenShortFlagCanBeUsedToSetTheEnvironmentForTokens() throws Exception
	{
		recreateBrjsWithMockVersionGenerator();
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(app).hasDefaultEnvironmentProperties("SOME.TOKEN", "token replacement")
				.and(app).hasEnvironmentProperties("myprod", "SOME.TOKEN", "prod replacement")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123");
		when(brjs).runCommand("build-app", "app", "-e", "myprod");
		then(brjs).fileContentsContains("generated/built-apps/app_myprod/v/123/js/prod/combined/bundle.js", "prod replacement");
	}

	@Test
	public void environmenLongFlagCanBeUsedToSetTheEnvironmentForTokens() throws Exception
	{
		recreateBrjsWithMockVersionGenerator();
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(app).hasDefaultEnvironmentProperties("SOME.TOKEN", "token replacement")
				.and(app).hasEnvironmentProperties("prod", "SOME.TOKEN", "prod replacement")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123");
		when(brjs).runCommand("build-app", "app", "--environment", "prod");
		then(brjs).fileContentsContains("generated/built-apps/app_prod/v/123/js/prod/combined/bundle.js", "prod replacement");
	}

	@Test
	public void exceptionIsThrownIfStaticAppTokenCannotBeReplaced() throws Exception
	{
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123");
		when(brjs).runCommand("build-app", "app");
		then(exceptions).verifyException(TokenReplacementException.class, "PropertyFileTokenFinder", "SOME.TOKEN");
	}
	
	@Test
	public void exceptionIsThrownIfStaticAppTokenCannotBeReplacedUsingADefinedEnvironment() throws Exception
	{
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123");
		when(brjs).runCommand("build-app", "app", "-e", "myprod");
		then(exceptions).verifyException(TokenReplacementException.class, "PropertyFileTokenFinder", "SOME.TOKEN");
	}
	
	@Test
	public void warningIsPrintedIfWarAppTokenCannotBeReplaced() throws Exception
	{
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(app).containsFolder("WEB-INF")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123")
				.and(logging).enabled();
		when(brjs).runCommand("build-app", "app", "-w");
		then(logging).unorderedWarnMessageReceived(LoggingMissingTokenHandler.NO_TOKEN_REPLACEMENT_MESSAGE, "SOME.TOKEN", "prod" )
			.and(logging).otherMessagesIgnored();
	}
	
	@Test
	public void jndiTokenWarningIsOnlyPrintedOnceForEachToken() throws Exception
	{
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en, de\n"
				+ "requirePrefix: appns")
				.and(app).containsFolder("WEB-INF")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@ @SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123")
				.and(logging).enabled();
		when(brjs).runCommand("build-app", "app", "-w");
		then(logging).unorderedWarnMessageReceived(LoggingMissingTokenHandler.NO_TOKEN_REPLACEMENT_MESSAGE, "SOME.TOKEN", "prod" )
			.and(logging).doesNotContainWarnMessage(LoggingMissingTokenHandler.NO_TOKEN_REPLACEMENT_MESSAGE, "SOME.TOKEN", "prod")
			.and(logging).otherMessagesIgnored();
	}
	
	@Test
	public void warningIsPrintedIfWarAppTokenCannotBeReplacedUsingADefinedEnvironment() throws Exception
	{
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(app).containsFolder("WEB-INF")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123")
				.and(logging).enabled();
		when(brjs).runCommand("build-app", "app", "-e", "myprod", "-w");
		then(logging).unorderedWarnMessageReceived(LoggingMissingTokenHandler.NO_TOKEN_REPLACEMENT_MESSAGE, "SOME.TOKEN", "myprod" )
			.and(logging).otherMessagesIgnored();
	}
	
	@Test
	public void exceptionIsThrownIfTokenCannotBeReplacedForBuiltWarWhereWebInfIsNotPresent() throws Exception
	{
		given(app).hasBeenCreated()
				.and(app).containsFileWithContents("app.conf", "localeCookieName: BRJS.LOCALE\n"
				+ "locales: en\n"
				+ "requirePrefix: appns")
				.and(defaultAspect).hasBeenCreated()
				.and(defaultAspect).containsFileWithContents("src/App.js", "@SOME.TOKEN@")
				.and(defaultAspect).indexPageHasContent("<@js.bundle@/>\n"+"require('appns/App');")
				.and(brjs).hasVersion("123");
		when(brjs).runCommand("build-app", "app", "-w");
		then(exceptions).verifyException(TokenReplacementException.class, "PropertyFileTokenFinder", "SOME.TOKEN");
	}
	
}
