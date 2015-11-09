package actions;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import main.MainWindow;
import org.jetbrains.annotations.NotNull;

public class CustomTypedHandlerDelegate extends TypedHandlerDelegate {

    char[] triggerSearchChars = new char[] {
      ' ', '.', ';'
    };

    @Override
    public Result charTyped(char c, Project project, @NotNull Editor editor, @NotNull PsiFile file) {

        for (char trigger : triggerSearchChars) {
            if (c == trigger) {
                int offset = editor.getCaretModel().getCurrentCaret().getOffset();
                int lastSpace = file.getText().lastIndexOf(' ', offset - 1);
                MainWindow.input.setText(file.getText().substring(lastSpace, offset));
                MainWindow.searchButton.doClick();
                break;
            }
        }
        return Result.CONTINUE;
    }

}
