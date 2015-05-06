package org.bladerunnerjs.legacy.command.test.testrunner;

import org.bladerunnerjs.api.App;
import org.bladerunnerjs.api.Aspect;
import org.bladerunnerjs.api.TestPack;
import org.bladerunnerjs.legacy.command.test.testrunner.specutility.BundlerHandlerSpecTest;
import org.bladerunnerjs.model.ThreadSafeStaticBRJSAccessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class BundlerHandlerTest extends BundlerHandlerSpecTest
{

	private App app;
	private Aspect aspect;
	private TestPack aspectTestPack;
	
	@Before
	public void initTestObjects() throws Exception
	{
		given(brjs).automaticallyFindsBundlerPlugins()
			.and(brjs).automaticallyFindsMinifierPlugins()
			.and(brjs).hasBeenCreated();
			app = brjs.app("app1");
			aspect = app.aspect("default");
			aspectTestPack = aspect.testType("unit").testTech("techy");
			
			ThreadSafeStaticBRJSAccessor.initializeModel(brjs);
	}
	
	@After
	public void tearDownThreadSafeModelAccessor() throws Exception
	{
		ThreadSafeStaticBRJSAccessor.destroy();
	}
	
	
	
	// New model request path tests
	
	@Test
	public void newJsRequestPathsCanBeUsedAsABundlePath() throws Exception
	{
		given(aspect).containsFileWithContents("src/appns/srcFile.js", "// some SDK src code")
			.and(aspectTestPack).containsFileWithContents("tests/test1.js", "require('appns/srcFile');");
		whenJstdTests(aspectTestPack).runWithPaths( "bundles/js/dev/combined/bundle.js" );
		thenJstdTests(aspectTestPack).testBundleContainsText(
					"bundles/js/dev/combined/bundle.js",
					"// some SDK src code" );
	}
	
	@Test
	public void newXmlRequestPathsCanBeUsedAsABundlePath() throws Exception
	{
		given(aspect).containsFileWithContents("src/appns/srcFile.js", "// some SDK src code")
			.and(aspect).containsFileWithContents("resources/file.xml", "<some xml>")
    		.and(aspectTestPack).containsFileWithContents("tests/test1.js", "require('appns/srcFile');");
    	whenJstdTests(aspectTestPack).runWithPaths( "bundles/xml/bundle.xml" );
    	thenJstdTests(aspectTestPack).testBundleContainsText(
    				"bundles/xml/bundle.xml",
    				"<some xml>" );
	}
	
	@Test
	public void newHtmlRequestPathsCanBeUsedAsABundlePath() throws Exception
	{
		given(aspect).containsFileWithContents("src/appns/srcFile.js", "// some SDK src code")
			.and(aspect).containsFileWithContents("resources/file.html", "<div id='appns.view'>TESTCONTENT</div>")
    		.and(aspectTestPack).containsFileWithContents("tests/test1.js", "require('appns/srcFile');");
    	whenJstdTests(aspectTestPack).runWithPaths( "bundles/html/bundle.html" );
    	thenJstdTests(aspectTestPack).testBundleContainsText(
    				"bundles/html/bundle.html",
    				"<div id='appns.view'>TESTCONTENT</div>" );
	}

	@Test
	public void newCssRequestPathsCanBeUsedAsABundlePath() throws Exception
	{
		given(aspect).containsFileWithContents("src/appns/srcFile.js", "// some SDK src code")
			.and(aspect).containsFileWithContents("resources/file.css", "some.css.styles { }")
    		.and(aspectTestPack).containsFileWithContents("tests/test1.js", "require('appns/srcFile');");
    	whenJstdTests(aspectTestPack).runWithPaths( "bundles/css/common/bundle.css" );
    	thenJstdTests(aspectTestPack).testBundleContainsText(
    				"bundles/css/common/bundle.css",
    				"some.css.styles { }" );
	}
	
	@Test
	public void newI18nRequestPathsCanBeUsedAsABundlePath() throws Exception
	{
		given(aspect).containsFileWithContents("src/appns/srcFile.js", "// some SDK src code")
			.and(aspect).containsFileWithContents("resources/en_GB.properties", "appns.prop = some prop")
    		.and(aspectTestPack).containsFileWithContents("tests/test1.js", "require('appns/srcFile');");
    	whenJstdTests(aspectTestPack).runWithPaths( "bundles/i18n/en_GB.js" );
    	thenJstdTests(aspectTestPack).testBundleContainsText(
    				"bundles/i18n/en_GB.js",
    				"\"appns.prop\": \"some prop\"" );
	}
	
	// Legacy bundle path tests
	
	@Test
	public void logicalRequestPathsDontPreventUseOfFullModelPaths() throws Exception
	{
		given(aspect).containsFileWithContents("src/appns/srcFile.js", "// some SDK src code\nvar a = function(){}")
			.and(aspectTestPack).containsFileWithContents("tests/test1.js", "require('appns/srcFile');");
		whenJstdTests(aspectTestPack).runWithPaths( "bundles/js/dev/closure-whitespace/bundle.js" );
		thenJstdTests(aspectTestPack).testBundleContainsText(
					"bundles/js/dev/closure-whitespace/bundle.js",
					"var a=function(){}" );
		thenJstdTests(aspectTestPack).testBundleDoesNotContainText(
				"bundles/js/dev/closure-whitespace/bundle.js",
				"// some SDK src code" );
	}
	
	@Test
	public void logicalJsRequestPathsCanBeUsedAsABundlePath() throws Exception
	{
		given(aspect).containsFileWithContents("src/appns/srcFile.js", "// some SDK src code")
			.and(aspectTestPack).containsFileWithContents("tests/test1.js", "require('appns/srcFile');");
		whenJstdTests(aspectTestPack).runWithPaths( "bundles/bundle.js" );
		thenJstdTests(aspectTestPack).testBundleContainsText(
					"bundles/bundle.js",
					"// some SDK src code" );
	}
	
	@Test
	public void bundlesCanBeGeneratedForTheDefaultAspect() throws Exception
	{
		given( app.defaultAspect() ).containsFileWithContents("src/appns/srcFile.js", "// some aspect src code")
			.and( app.defaultAspect().testType("UT").testTech("tech") ).containsFileWithContents("tests/test1.js", "require('appns/srcFile');");
		whenJstdTests( app.defaultAspect().testType("UT").testTech("tech") ).runWithPaths( "bundles/bundle.js" );
		thenJstdTests( app.defaultAspect().testType("UT").testTech("tech") ).testBundleContainsText(
					"bundles/bundle.js",
					"// some aspect src code" );
	}
	
	@Test
	public void weCanUseUTF8() throws Exception {
		given(brjs.bladerunnerConf()).defaultFileCharacterEncodingIs("UTF-8")
			.and().activeEncodingIs("UTF-8")
			.and(aspect).containsFileWithContents("src/appns/Class1.js", "// $£€")
			.and(aspect.testType("UT").testTech("tech")).testFileHasContent("TestClass.js", "require('appns/Class1')");
		whenJstdTests( aspect.testType("UT").testTech("tech") ).runWithPaths( "bundles/bundle.js" );
		thenJstdTests( aspect.testType("UT").testTech("tech") ).testBundleContainsText(
					"bundles/bundle.js",
					"// $£€" );
	}
	
	@Test
	public void weCanUseLatin1() throws Exception {
		given(brjs.bladerunnerConf()).defaultFileCharacterEncodingIs("ISO-8859-1")
    		.and().activeEncodingIs("ISO-8859-1")
    		.and(aspect).containsFileWithContents("src/appns/Class1.js", "// $£")
    		.and(aspect.testType("UT").testTech("tech")).testFileHasContent("TestClass.js", "require('appns/Class1')");
    	whenJstdTests( aspect.testType("UT").testTech("tech") ).runWithPaths( "bundles/bundle.js" );
    	thenJstdTests( aspect.testType("UT").testTech("tech") ).testBundleContainsText(
    				"bundles/bundle.js",
    				"// $£" );
	}
	
	@Test
	public void weCanUseUnicodeFilesWithABomMarkerEvenWhenThisIsNotTheDefaultEncoding() throws Exception {
		given(brjs.bladerunnerConf()).defaultFileCharacterEncodingIs("ISO-8859-1")
    		.and().activeEncodingIs("UTF-16")
    		.and(aspect).containsFileWithContents("src/appns/Class1.js", "// $£€")
    		.and(aspect.testType("UT").testTech("tech")).testFileHasContent("TestClass.js", "require('appns/Class1')");
    	whenJstdTests( aspect.testType("UT").testTech("tech") ).runWithPaths( "bundles/bundle.js" );
    	thenJstdTests( aspect.testType("UT").testTech("tech") ).testBundleContainsText(
    				"bundles/bundle.js",
    				"// $£€" );
	}
	
	@Test
	public void allPossibleBundlePathAliasesAreValidModelRequests() throws Exception
	{
		whenJstdTests(aspectTestPack).runWithPaths( 
				"bundles/bundle.js",
				"bundles/bundle.css",
				"bundles/bundle.i18n",
				"bundles/bundle.xml",
				"bundles/bundle.html");
		then(exceptions).verifyNoOutstandingExceptions();
	}
	
}
