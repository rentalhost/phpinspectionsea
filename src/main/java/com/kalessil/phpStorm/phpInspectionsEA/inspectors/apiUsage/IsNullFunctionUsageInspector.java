package com.kalessil.phpStorm.phpInspectionsEA.inspectors.apiUsage;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.AssignmentExpression;
import com.jetbrains.php.lang.psi.elements.BinaryExpression;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.UnaryExpression;
import com.kalessil.phpStorm.phpInspectionsEA.fixers.UseSuggestedReplacementFixer;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.options.OptionsComponent;
import com.kalessil.phpStorm.phpInspectionsEA.utils.OpenapiElementsUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.OpenapiTypesUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.PhpLanguageUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/*
 * This file is part of the Php Inspections (EA Extended) package.
 *
 * (c) Vladimir Reznichenko <kalessil@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

public class IsNullFunctionUsageInspector extends BasePhpInspection {
    // Inspection options.
    public boolean PREFER_YODA_STYLE    = true;
    public boolean PREFER_REGULAR_STYLE = false;

    private static final String messagePattern = "'%e%' construction should be used instead.";

    @NotNull
    public String getShortName() {
        return "IsNullFunctionUsageInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            @Override
            public void visitPhpFunctionCall(@NotNull FunctionReference reference) {
                final String functionName = reference.getName();
                if (functionName == null || !functionName.equals("is_null")) {
                    return;
                }
                final PsiElement[] arguments = reference.getParameters();
                if (arguments.length != 1) {
                    return;
                }

                final PsiElement parent = reference.getParent();

                /* check the context */
                boolean checksIsNull = true;
                PsiElement target    = reference;
                if (parent instanceof UnaryExpression) {
                    if (OpenapiTypesUtil.is(((UnaryExpression) parent).getOperation(), PhpTokenTypes.opNOT)) {
                        checksIsNull = false;
                        target       = parent;
                    }
                } else if (parent instanceof BinaryExpression) {
                    /* extract isnulls' expression parts */
                    final BinaryExpression expression = (BinaryExpression) parent;
                    final PsiElement secondOperand    = OpenapiElementsUtil.getSecondOperand(expression, reference);
                    if (PhpLanguageUtil.isBoolean(secondOperand)) {
                        final IElementType operation = expression.getOperationType();
                        if (PhpTokenTypes.opEQUAL == operation || PhpTokenTypes.opIDENTICAL == operation) {
                            target       = parent;
                            checksIsNull = PhpLanguageUtil.isTrue(secondOperand);
                        } else if (operation == PhpTokenTypes.opNOT_EQUAL || operation == PhpTokenTypes.opNOT_IDENTICAL) {
                            target       = parent;
                            checksIsNull = !PhpLanguageUtil.isTrue(secondOperand);
                        } else {
                            target = reference;
                        }
                    }
                }

                /* report the issue */
                final boolean wrapArgument = PREFER_REGULAR_STYLE && arguments[0] instanceof AssignmentExpression;
                final String replacement   = (PREFER_YODA_STYLE ? "null %o% %a%" : "%a% %o% null")
                        .replace("%o%", checksIsNull ? "===" : "!==")
                        .replace("%a%", wrapArgument ? "(%a%)" : "%a%")
                        .replace("%a%", arguments[0].getText());
                final String message       = messagePattern.replace("%e%", replacement);
                holder.registerProblem(target, message, new CompareToNullFix(replacement));
            }
        };
    }

    public JComponent createOptionsPanel() {
        return OptionsComponent.create((component) -> component.delegateRadioCreation((radioComponent) -> {
            radioComponent.addOption("Regular fix style", PREFER_REGULAR_STYLE, (isSelected) -> PREFER_REGULAR_STYLE = isSelected);
            radioComponent.addOption("Yoda fix style", PREFER_YODA_STYLE, (isSelected) -> PREFER_YODA_STYLE = isSelected);
        }));
    }

    private static final class CompareToNullFix extends UseSuggestedReplacementFixer {
        private static final String title = "Use null comparison instead";

        @NotNull
        @Override
        public String getName() {
            return title;
        }

        CompareToNullFix(@NotNull String expression) {
            super(expression);
        }
    }
}