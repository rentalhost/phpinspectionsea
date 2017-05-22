package com.kalessil.phpStorm.phpInspectionsEA.inspectors.codeSmell;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.BinaryExpression;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.impl.PhpExpressionImpl;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.BinaryExpressionUtil;

import org.jetbrains.annotations.NotNull;

public class MagicNumberInspector extends BasePhpInspection {
    private static final String message = "Magic number should be replaced by a constant.";

    @NotNull
    public String getShortName() {
        return "MagicNumberInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder problemsHolder, final boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            @Override
            public void visitPhpExpression(final PhpExpression expression) {
                if (isNumeric(expression)) {
                    if (!(expression.getParent() instanceof PhpReturn)) {
                        return;
                    }

                    registerProblem(expression);
                }
            }

            @Override
            public void visitPhpBinaryExpression(final BinaryExpression expression) {
                if (!BinaryExpressionUtil.isComparison(expression)) {
                    return;
                }

                final PsiElement leftOperand  = expression.getLeftOperand();
                final PsiElement rightOperand = expression.getRightOperand();

                if (isNumeric(leftOperand) &&
                    !isZeroComparedToFunctionReference(leftOperand, rightOperand)) {
                    registerProblem(leftOperand);
                }

                if (isNumeric(rightOperand) &&
                    !isZeroComparedToFunctionReference(rightOperand, leftOperand)) {
                    registerProblem(rightOperand);
                }
            }

            private boolean isNumeric(final PsiElement expression) {
                return (expression instanceof PhpExpressionImpl) &&
                       (expression.getNode().getElementType() == PhpElementTypes.NUMBER);
            }

            private boolean isZeroComparedToFunctionReference(@NotNull final PsiElement primaryOperand, final PsiElement oppositeOperand) {
                return "0".equals(primaryOperand.getText()) &&
                       (oppositeOperand instanceof FunctionReference);
            }

            private void registerProblem(final PsiElement rightOperand) {
                problemsHolder.registerProblem(rightOperand, message, ProblemHighlightType.WEAK_WARNING);
            }
        };
    }
}
