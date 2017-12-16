package com.kalessil.phpStorm.phpInspectionsEA.security;

import com.intellij.codeInsight.intention.IntentionAction;
import com.kalessil.phpStorm.phpInspectionsEA.PhpCodeInsightFixtureTestCase;
import com.kalessil.phpStorm.phpInspectionsEA.inspectors.security.NonSecureHtmlentitiesUsageInspector;

final public class NonSecureHtmlentitiesUsageInspectorTest extends PhpCodeInsightFixtureTestCase {
    public void testIfFindsAllPatterns() {
        myFixture.enableInspections(new NonSecureHtmlentitiesUsageInspector());
        myFixture.configureByFile("fixtures/security/htmlentities.php");
        myFixture.testHighlighting(true, false, true);

        for (final IntentionAction fix : myFixture.getAllQuickFixes()) {
            myFixture.launchAction(fix);
        }
        myFixture.setTestDataPath(".");
        myFixture.checkResultByFile("fixtures/security/htmlentities.fixed.php");
    }
}

