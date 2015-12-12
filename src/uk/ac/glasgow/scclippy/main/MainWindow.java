package uk.ac.glasgow.scclippy.main;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import uk.ac.glasgow.scclippy.lucene.File;
import uk.ac.glasgow.scclippy.lucene.SearchFiles;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

/**
 * Main Tool Window class
 */
public class MainWindow implements ToolWindowFactory {

    private static int MAIN_SCROLL_STEP = 10;
    private static int INPUT_TEXT_AREA_ROWS = 5; //TODO make this auto resizable

    static int queryNumber = 5; // TODO make input for this
    private static String indexPath = "D:/index";

    public static JTextArea input = new JTextArea();
    public static JEditorPane[] output = new JEditorPane[queryNumber];

    public static JButton searchButton = new JButton("Search");

    public static Editor currentEditor;

    JPanel resultPanel = new JPanel();
    JScrollPane resultPanelScroll = new JBScrollPane(resultPanel);

    File[] files = null;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        // TODO add select and create index options
        // IndexFiles.index(new String[] {"-index", "D:/sccindex/index", "-docs", "D:/sccdata", "-update"});

        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.PAGE_AXIS));
        resultPanelScroll.getVerticalScrollBar().setUnitIncrement(MAIN_SCROLL_STEP);

        Component component = toolWindow.getComponent();
        component.getParent().add(resultPanelScroll);

        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, JBColor.CYAN));
        input.setRows(INPUT_TEXT_AREA_ROWS);

        JScrollPane inputScrollPane = new JBScrollPane(input);
        resultPanel.add(inputScrollPane);

        searchButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            files = SearchFiles.search(new String[]{
                                    indexPath, "contents", input.getText(), String.valueOf(queryNumber)
                            });
                        } catch (Exception e) {
                            /* TODO: Show intellij notification for failure */
                            System.err.println(e.getMessage());
                        }

                        if (files == null)
                            return;

                        for (int i = 0; i < files.length; i++) {
                            String text = files[i].getContent();
                            String url = "<a href=\"http://stackoverflow.com/questions/"
                                    + files[i].getFileName()
                                    + "\">Link to Stackoverflow</a>";
                            output[i].setText(text + url);
                            output[i].setEnabled(true);
                            output[i].updateUI();
                        }
                        for (int i = files.length; i < output.length; i++) {
                            output[i].setText("");
                        }
                    }
                }
        );
        resultPanel.add(searchButton);

        for (int i = 0; i < output.length; i++) {
            output[i] = new JEditorPane("text/html", "");
            output[i].setEditable(false);
            output[i].setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, JBColor.YELLOW));
            HTMLEditorKit kit = new HTMLEditorKit();
            output[i].setEditorKit(kit);
            kit.getStyleSheet().addRule("code {background-color: olive;}");

            output[i].addMouseListener(new SelectedSnippetListener(i, toolWindow));
            output[i].addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        if (Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (URISyntaxException e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                }
            });
            resultPanel.add(output[i]);
        }
    }

    private class SelectedSnippetListener extends MouseAdapter {
        int id;
        ToolWindow toolWindow;

        public SelectedSnippetListener(int id, ToolWindow toolWindow) {
            this.id = id;
            this.toolWindow = toolWindow;
        }

        @Override
        public void mouseClicked(MouseEvent e) {

            // double click
            if (e.getClickCount() == 2 && files != null && files[id] != null) {
                String text = files[id].getContent();
                int start, end = 0;
                String startText = "<code>";
                String endText = "</code>";

                List<String> snippets = new LinkedList<>();
                while ((start = text.indexOf(startText, end)) != -1 &&
                        (end = text.indexOf(endText, start)) != -1) {
                    snippets.add(text.substring(start + startText.length(), end));
                }

                if (snippets.size() == 1) {
                    // insert directly
                    insertTextIntoEditor(snippets.get(0));
                } else if (snippets.size() > 1) {
                    // ask user for input
                    JPanel panel = new JPanel();

                    JLabel snippetsLabel = new JLabel();
                    panel.add(snippetsLabel);

                    int inputDialogMaxSnippetLength = 100;
                    Object[] possibilities = new Object[snippets.size()];
                    for (int i = 0; i < snippets.size(); i++) {
                        if (snippets.get(i).length() > 100)
                            possibilities[i] = (i+1) + ":" + snippets.get(i).substring(0, inputDialogMaxSnippetLength);
                        else
                            possibilities[i] = (i+1) + ":" + snippets.get(i);
                    }

                    String chosenSnippet = (String) JOptionPane.showInputDialog(
                            resultPanel,
                            "Choose which code snippet:\n",
                            "Code snippet",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            possibilities,
                            possibilities[0]);

                    if ((chosenSnippet != null) && (chosenSnippet.length() > 0)) {
                        int index = chosenSnippet.indexOf(":");
                        insertTextIntoEditor(snippets.get(Integer.parseInt(chosenSnippet.substring(0, index)) - 1));
                    }

                }
            }
        }

        private void insertTextIntoEditor(String text) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    if (currentEditor == null)
                        return;

                    Document doc = currentEditor.getDocument();
                    int offset = currentEditor.getCaretModel().getOffset();

                    doc.setText(
                            doc.getText(new TextRange(0, offset)) +
                            text +
                            doc.getText(new TextRange(offset, doc.getText().length()))
                    );
                }
            });
        }
    }
}
