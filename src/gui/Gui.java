package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTabbedPane;
import javax.swing.JTree;

import parser.Parser;
import semanticAnalyzer.SymbolTableItem;
import lexer.Lexer;
import lexer.Token;
import gui.NumeroLinea;
import java.awt.Desktop;
import java.awt.Event;
import java.io.FileWriter;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.text.DefaultEditorKit;

public class Gui extends JFrame implements ActionListener {

    private JTextArea console, codeArea;
    private JTextArea editor;
    private JTable tokensTable;
    private JTable semanticTable;
    //opciones menu archivo
    private JMenuItem menuOpen = new JMenuItem("Abrir Archivo.");
    private JMenuItem menuSave = new JMenuItem("Guardar Archivo.");
    private JMenuItem menuClose = new JMenuItem("Salir.");
    private JMenuItem menuNew = new JMenuItem("Nuevo Archivo.");

    // opciones menu compilar
    private JMenuItem menuCompiler = new JMenuItem("Compilar");
    //opciones menu ayuda
    private JMenuItem menuManual = new JMenuItem("Manual de usuario");
    private JMenuItem menuDoc = new JMenuItem("Documentacion");
    private JMenuItem menuAbout = new JMenuItem("Acerca de");

    private JTree tree;
    private JPanel treePanel = new JPanel(new GridLayout(1, 1));
    private JScrollPane scroll;


    public void writeCode(String msg) {
        codeArea.append(msg + "\n");
    }

    public void writeConsole(String msg) {
        console.append(msg + "\n");
    }

    private void writeEditor(String msg) {
        editor.append(msg + "\n");

    }

    private void writeTokenTable(Vector<Token> tokens) {
        for (Token token1 : tokens) {
            int line = token1.getLine();
            String token = token1.getToken();
            String word = token1.getWord();
            ((DefaultTableModel) tokensTable.getModel()).addRow(new Object[]{String.format("%04d", line), token, word});
        }
    }

    public void writeSymbolTable(Hashtable<String, Vector<SymbolTableItem>> symbolTable) {
        if (symbolTable == null) {
            return;
        }
        Enumeration items = symbolTable.keys();
        if (items != null) {
            while (items.hasMoreElements()) {
                String name = (String) items.nextElement();
                String type = ((SymbolTableItem) (symbolTable.get(name).get(0))).getType();
                String scope = ((SymbolTableItem) (symbolTable.get(name).get(0))).getScope();
                String value = ((SymbolTableItem) (symbolTable.get(name).get(0))).getValue();
                ((DefaultTableModel) semanticTable.getModel()).addRow(new Object[]{name, type, scope,});
            }
        }

    }

    private void clearTokenTable() {
        int ta = ((DefaultTableModel) tokensTable.getModel()).getRowCount();
        for (int i = 0; i < ta; i++) {
            ((DefaultTableModel) tokensTable.getModel()).removeRow(0);
        }
    }

    private void clearSemanticTable() {
        int ta = ((DefaultTableModel) semanticTable.getModel()).getRowCount();
        for (int i = 0; i < ta; i++) {
            ((DefaultTableModel) semanticTable.getModel()).removeRow(0);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //abrir archivo
        if (menuOpen.equals(e.getSource())) {
            JFileChooser fc = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt", "text");
            fc.setFileFilter(filter);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                console.setText("");
                codeArea.setText("");
                editor.setText("");
                clearTokenTable();
                clearSemanticTable();
                try {
                    loadFile(file.getAbsolutePath());
                } catch (IOException ex) {
                    writeConsole(ex.toString());
                }
            }
            //ejecuta 
        } else if (menuCompiler.equals(e.getSource())) {
            clearTokenTable();
            clearSemanticTable();
            console.setText("");
            codeArea.setText("");
            // analisis lexico
            if (editor.getText().equals("")) {
                writeConsole("El archivo esta vacio");
                return;
            }
            Lexer lex = new Lexer(editor.getText());

            lex.run();
            Vector<Token> tokens = lex.getTokens();
            // ver token en la tabla
            writeTokenTable(tokens);
            // contando errores
            int errors = 0;
            for (Token token : tokens) {
                if (token.getToken().equals("ERROR")) {
                    errors++;
                }
            }
            // ver los estados en la consola
            writeConsole(tokens.size() + " cadenas encontradas en: " + tokens.get(tokens.size() - 1).getLine() + " lineas,");
            writeConsole(errors + " cadenas no coinciden con ninguna regla");
            // actualiza el arbol
            treePanel.removeAll();
            tree = new JTree(Parser.run(tokens, this));
            JScrollPane treeView = new JScrollPane(tree);
            // expande a mas nodos
            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }
            treePanel.add(treeView);
            treePanel.revalidate();
            treePanel.repaint();
        } else if (menuNew.equals(e.getSource())) {
            //crea archivo sin nombre
            console.setText("");
            codeArea.setText("");
            editor.setText("");
            clearTokenTable();
            clearSemanticTable();
        } else if (menuSave.equals(e.getSource())) {
            //uarda un nuevo archivo txt
            Exportar();
        } else if (menuClose.equals(e.getSource())) {
            //salir de programa
            System.exit(0);
        } else if (menuManual.equals(e.getSource())) {
            try {
                File path = new File("src/utils/manual.pdf");
                Desktop.getDesktop().open(path);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else if (menuDoc.equals(e.getSource())) {
            try {
                File path = new File("src/utils/documentacion.pdf");
                Desktop.getDesktop().open(path);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else if (menuAbout.equals(e.getSource())) {
            JOptionPane.showMessageDialog(this, "Realizado por: \nCecilia Anonio Bautista \nDaniela Osorio Osorio \nJose Antonio Osorio Osorio \n\nEquipo num 7 \n\nMateria: \nLenguaje y automatas II \n\n Docente:\nManuel Hernandez Hernandez", "Acerca de", JOptionPane.INFORMATION_MESSAGE);
        }

    }

    private boolean loadFile(String file) throws FileNotFoundException, IOException {
        String line;
        BufferedReader br = new BufferedReader(new FileReader(file));
        writeConsole("Leyendo " + file + "");
        line = br.readLine();
        while (line != null) {
            writeEditor(line);
            line = br.readLine();
        }
        writeConsole("Archivo Cargado.");
        br.close();
        return true;
    }

    public void Exportar() {
        try {
            JFileChooser archivo = new JFileChooser(System.getProperty("user.dir"));
            archivo.showSaveDialog(this);
            if (archivo.getSelectedFile() != null) {
                try ( FileWriter guardado = new FileWriter(archivo.getSelectedFile()+".txt")) {
                    guardado.write(editor.getText());
                    JOptionPane.showMessageDialog(rootPane, "El archivo fue guardado con éxito en la ruta establecida");
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFile = new JMenu("Archivo");
        JMenu menuRun = new JMenu("Ejecutar");
        JMenu menuEdit = new JMenu("Editar");
        JMenu menuHelp = new JMenu("Ayuda");

        //opciones menu archivo
        menuOpen.addActionListener(this);
        menuSave.addActionListener(this);
        menuClose.addActionListener(this);
        menuNew.addActionListener(this);
        // opciones menu compilar
        menuCompiler.addActionListener(this);
        //opciones menu ayuda
        menuManual.addActionListener(this);
        menuDoc.addActionListener(this);
        menuAbout.addActionListener(this);

        //opciones menu archivo
        menuFile.add(menuNew);
        menuFile.add(menuOpen);
        menuFile.add(menuSave);
        menuFile.add(menuClose);
        // opciones menu edicion
        Action cutAction = new DefaultEditorKit.CutAction();
        cutAction.putValue(Action.NAME, "Cortar");
        cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('X', CTRL_DOWN_MASK));
        menuEdit.add(cutAction);

        Action copyAction = new DefaultEditorKit.CopyAction();
        copyAction.putValue(Action.NAME, "Copiar");
        copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('C', CTRL_DOWN_MASK));
        menuEdit.add(copyAction);

        Action pasteAction = new DefaultEditorKit.PasteAction();
        pasteAction.putValue(Action.NAME, "Pegar");
        pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('V', CTRL_DOWN_MASK));
        menuEdit.add(pasteAction);
        // opciones menu compilar
        menuRun.add(menuCompiler);
        //opciones menu ayuda
        menuHelp.add(menuManual);
        menuHelp.add(menuDoc);
        menuHelp.add(menuAbout);

        //añadir menus
        menuBar.add(menuFile);
        menuBar.add(menuEdit);
        menuBar.add(menuRun);
        menuBar.add(menuHelp);
        setJMenuBar(menuBar);
    }

    private void createGUI() {
        TitledBorder panelTitle;
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        JPanel downPanel = new JPanel(new GridLayout(1, 1));
        JPanel tokenPanel = new JPanel(new GridLayout(1, 1));
        JPanel semanticPanel = new JPanel(new GridLayout(1, 1));
        JPanel screenPanel = new JPanel(new GridLayout(1, 1));
        JPanel consolePanel = new JPanel(new GridLayout(1, 1));
        JPanel codePanel = new JPanel(new GridLayout(1, 1));

        // editor
        panelTitle = BorderFactory.createTitledBorder("Codigo fuente:");
        screenPanel.setBorder(panelTitle);
        editor = new JTextArea();
        editor.setEditable(true);
        scroll = new JScrollPane(editor);
        screenPanel.add(scroll);
        //numero de lineas jTextArea
        NumeroLinea nl = new NumeroLinea(editor);
        scroll.setRowHeaderView(nl);
        // tokens 
        panelTitle = BorderFactory.createTitledBorder("Analisis Lexico");
        tokenPanel.setBorder(panelTitle);
        DefaultTableModel modelRegistry = new DefaultTableModel();
        tokensTable = new JTable(modelRegistry);
        tokensTable.setShowGrid(true);
        tokensTable.setGridColor(Color.LIGHT_GRAY);
        tokensTable.setAutoCreateRowSorter(true);
        modelRegistry.addColumn("linea");
        modelRegistry.addColumn("token");
        modelRegistry.addColumn("cadena o palabra");
        JScrollPane scrollRegistry = new JScrollPane(tokensTable);
        tokensTable.setFillsViewportHeight(true);
        tokenPanel.add(scrollRegistry);
        tokensTable.setEnabled(false);
        // consola
        panelTitle = BorderFactory.createTitledBorder("Consola");
        consolePanel.setBorder(panelTitle);
        console = new JTextArea();
        console.setEditable(false);
        console.setBackground(Color.white);
        console.setForeground(Color.red);
        JScrollPane scrollConsole = new JScrollPane(console);
        consolePanel.add(scrollConsole);
        // arbol
        panelTitle = BorderFactory.createTitledBorder("Analisis Sintactico");
        treePanel.setBorder(panelTitle);
        JScrollPane treeView = new JScrollPane(new JLabel("Después de la compilación, el árbol de análisis se mostrará aquí", JLabel.CENTER));
        treePanel.add(treeView);
        // analisis semantico
        panelTitle = BorderFactory.createTitledBorder("Tabla de simbolos");
        semanticPanel.setBorder(panelTitle);
        DefaultTableModel modelSemantic = new DefaultTableModel();
        semanticTable = new JTable(modelSemantic);
        semanticTable.setShowGrid(true);
        semanticTable.setGridColor(Color.LIGHT_GRAY);
        semanticTable.setAutoCreateRowSorter(true);
        modelSemantic.addColumn("Nombre");
        modelSemantic.addColumn("Tipo");
        modelSemantic.addColumn("dimensión");
        modelSemantic.addColumn("Valpr");
        JScrollPane scrollSemantic = new JScrollPane(semanticTable);
        semanticTable.setFillsViewportHeight(true);
        semanticPanel.add(scrollSemantic);
        semanticTable.setEnabled(false);
        // codigo generador
        panelTitle = BorderFactory.createTitledBorder("Codigo objeto");
        codePanel.setBorder(panelTitle);
        codeArea = new JTextArea();
        JScrollPane scrollCode = new JScrollPane(codeArea);
        codePanel.add(scrollCode);
        // tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("lexico", tokenPanel);
        tabbedPane.addTab("sintactico", treePanel);
        tabbedPane.addTab(" analizador semantico", semanticPanel);
        tabbedPane.addTab("Codigo objeto", codePanel);
        tabbedPane.setSelectedIndex(3);
        // main frame
        topPanel.add(screenPanel);
        topPanel.add(tabbedPane);
        downPanel.add(consolePanel);
        downPanel.setPreferredSize(new Dimension(getWidth(), getHeight() / 4));
        add(topPanel, BorderLayout.CENTER);
        add(downPanel, BorderLayout.SOUTH);
        // editor hotkey
        //opciones menu archivo
        menuOpen.setAccelerator(KeyStroke.getKeyStroke('O', CTRL_DOWN_MASK));
        menuSave.setAccelerator(KeyStroke.getKeyStroke('S', CTRL_DOWN_MASK));
        menuClose.setAccelerator(KeyStroke.getKeyStroke('Q', CTRL_DOWN_MASK));
        menuNew.setAccelerator(KeyStroke.getKeyStroke('N', CTRL_DOWN_MASK));

        // opciones menu compilar
        menuCompiler.setAccelerator(KeyStroke.getKeyStroke('R', CTRL_DOWN_MASK));
        //opciones menu ayuda
        menuManual.setAccelerator(KeyStroke.getKeyStroke('H', CTRL_DOWN_MASK));
        menuDoc.setAccelerator(KeyStroke.getKeyStroke('L', CTRL_DOWN_MASK));
        menuAbout.setAccelerator(KeyStroke.getKeyStroke('I', CTRL_DOWN_MASK));

    }

    public Gui(String title) throws IOException {
        super(title);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
        }
        Dimension dim = getToolkit().getScreenSize();
        setSize(3 * dim.width / 4, 3 * dim.height / 4);
        setLocation((dim.width - getSize().width) / 2, (dim.height - getSize().height) / 2);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        createMenu();
        createGUI();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {

        Gui gui = new Gui("U1PR00_Parser_CompilerProgram_EQ07");
        gui.setVisible(true);
    }

}
