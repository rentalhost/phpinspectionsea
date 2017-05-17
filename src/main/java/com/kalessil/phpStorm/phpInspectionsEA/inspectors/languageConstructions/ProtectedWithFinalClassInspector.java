package com.kalessil.phpStorm.phpInspectionsEA.inspectors.languageConstructions;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;

import org.jetbrains.annotations.NotNull;

public class ProtectedWithFinalClassInspector extends BasePhpInspection {
    private static final String message = "Protected modifier could be replaced by private.";

    @NotNull
    public String getShortName() {
        return "ProtectedWithFinalClassInspector";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder problemsHolder, final boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            private void checkElement(final PhpElementWithModifier element, final PsiElement elementModifierScope) {
                if (!element.getModifier().isProtected()) {
                    return;
                }

                final PhpClass elementClass = ((PhpClassMember) element).getContainingClass();

                assert elementClass != null;

                if (!elementClass.isFinal()) {
                    return;
                }

                final PhpModifierList  elementModifierList = PsiTreeUtil.findChildOfType(elementModifierScope, PhpModifierList.class);
                final LeafPsiElement[] elementModifiers    = PsiTreeUtil.getChildrenOfType(elementModifierList, LeafPsiElement.class);

                assert elementModifiers != null;

                for (final LeafPsiElement elementModifier : elementModifiers) {
                    if ("protected".equalsIgnoreCase(elementModifier.getText())) {
                        problemsHolder.registerProblem(elementModifier, message, ProblemHighlightType.WEAK_WARNING,
                                                       new TheLocalFix(elementModifier));
                        break;
                    }
                }
            }

            @Override
            public void visitPhpField(final Field field) {
                // Note: it does the work for properties and constants.
                checkElement(field, field.getParent());
            }

            @Override
            public void visitPhpMethod(final Method method) {
                checkElement(method, method);
            }
        };
    }

    private static class TheLocalFix implements LocalQuickFix {
        private final SmartPsiElementPointer<LeafPsiElement> modifier;

        TheLocalFix(@NotNull final LeafPsiElement modifierElement) {
            final SmartPointerManager manager = SmartPointerManager.getInstance(modifierElement.getProject());

            modifier = manager.createSmartPsiElementPointer(modifierElement);
        }

        @NotNull
        @Override
        public String getName() {
            return "Declare private";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor descriptor) {
            final PsiElement modifierElement     = modifier.getElement();
            final PsiElement modifierReplacement = PhpPsiElementFactory.createFromText(project, LeafPsiElement.class, "private");

            assert modifierElement != null;
            assert modifierReplacement != null;

            modifierElement.replace(modifierReplacement);
        }
    }
}
