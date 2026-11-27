package com.codenote;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

/** CodeNote: a lightweight VS Code-inspired Java editor built with Swing. */
public class CodeNoteApp {
    private JFrame frame;
    private JTabbedPane tabs;
    private JTextArea terminal;
    private JTextField commandField;
    private JTree fileTree;
    private JPanel explorerPanel, terminalPanel;
    private boolean dark = true;
    private Path workspace;
    private final Map<Component, EditorTab> editors = new HashMap<>();
    private final Color DARK_BG = new Color(30,30,30), DARK_PANEL = new Color(37,37,38), DARK_FG = new Color(220,220,220);
    private final Color LIGHT_BG = new Color(250,250,250), LIGHT_PANEL = new Color(238,238,238), LIGHT_FG = new Color(35,35,35);

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new CodeNoteApp().start()); }

    private void start() {
        frame = new JFrame("CodeNote — Java Editor");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(1200, 780); frame.setMinimumSize(new Dimension(850, 560));
        frame.setLocationRelativeTo(null);
        tabs = new JTabbedPane(); tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        explorerPanel = new JPanel(new BorderLayout()); explorerPanel.setPreferredSize(new Dimension(230, 500));
        JLabel exTitle = new JLabel("  EXPLORER"); exTitle.setBorder(new EmptyBorder(10,8,10,4)); exTitle.setFont(exTitle.getFont().deriveFont(Font.BOLD, 12f));
        JButton openFolder = new JButton("열기"); openFolder.addActionListener(e -> chooseWorkspace());
        JPanel exTop = new JPanel(new BorderLayout()); exTop.add(exTitle, BorderLayout.CENTER); exTop.add(openFolder, BorderLayout.EAST);
        explorerPanel.add(exTop, BorderLayout.NORTH);
        fileTree = new JTree(new DefaultMutableTreeNode("폴더를 열어 주세요")); fileTree.setRootVisible(true);
        fileTree.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){ if(e.getClickCount()==2){ Object o=fileTree.getLastSelectedPathComponent(); if(o instanceof DefaultMutableTreeNode n && n.getUserObject() instanceof File f && f.isFile()) openFile(f.toPath()); } }});
        explorerPanel.add(new JScrollPane(fileTree), BorderLayout.CENTER);

        terminal = new JTextArea(8, 80); terminal.setEditable(false); terminal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandField = new JTextField(); commandField.addActionListener(e -> runShellCommand());
        JButton runCmd = new JButton("실행"); runCmd.addActionListener(e -> runShellCommand());
        terminalPanel = new JPanel(new BorderLayout()); terminalPanel.setPreferredSize(new Dimension(700, 170));
        JLabel termTitle = new JLabel("  TERMINAL"); termTitle.setBorder(new EmptyBorder(7,8,7,4));
        JPanel termInput = new JPanel(new BorderLayout(5,0)); termInput.add(new JLabel(" > "),BorderLayout.WEST); termInput.add(commandField,BorderLayout.CENTER); termInput.add(runCmd,BorderLayout.EAST);
        terminalPanel.add(termTitle,BorderLayout.NORTH); terminalPanel.add(new JScrollPane(terminal),BorderLayout.CENTER); terminalPanel.add(termInput,BorderLayout.SOUTH);

        JSplitPane work = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, explorerPanel, tabs); work.setDividerLocation(230);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, work, terminalPanel); mainSplit.setResizeWeight(.78); mainSplit.setDividerLocation(570);
        frame.add(mainSplit, BorderLayout.CENTER); frame.setJMenuBar(createMenu());
        addNewTab(); applyTheme();
        frame.addWindowListener(new WindowAdapter(){ public void windowClosing(WindowEvent e){ if(confirmCloseAll()) { saveAll(); frame.dispose(); } }});
        frame.setVisible(true);
        appendTerminal("CodeNote 준비 완료. Java " + Runtime.version().feature() + " 감지됨.\n");
    }

    private JMenuBar createMenu(){
        JMenuBar bar=new JMenuBar(); JMenu file=new JMenu("파일");
        item(file,"새 파일",KeyStroke.getKeyStroke(KeyEvent.VK_N,InputEvent.CTRL_DOWN_MASK),e->addNewTab());
        item(file,"파일 열기",KeyStroke.getKeyStroke(KeyEvent.VK_O,InputEvent.CTRL_DOWN_MASK),e->chooseFile());
        item(file,"저장",KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK),e->saveCurrent());
        item(file,"다른 이름으로 저장",null,e->saveAsCurrent()); file.addSeparator(); item(file,"폴더 열기",null,e->chooseWorkspace());
        JMenu edit=new JMenu("편집"); item(edit,"찾기",KeyStroke.getKeyStroke(KeyEvent.VK_F,InputEvent.CTRL_DOWN_MASK),e->showFind());
        JMenu view=new JMenu("보기"); item(view,"테마 전환",null,e->{dark=!dark;applyTheme();}); item(view,"터미널 표시/숨기기",null,e->terminalPanel.setVisible(!terminalPanel.isVisible()));
        JMenu run=new JMenu("실행"); item(run,"현재 Java 파일 실행",KeyStroke.getKeyStroke(KeyEvent.VK_F6,0),e->compileAndRun());
        bar.add(file);bar.add(edit);bar.add(view);bar.add(run);return bar;
    }
    private void item(JMenu m,String name,KeyStroke key, java.util.function.Consumer<ActionEvent> action){ JMenuItem i=new JMenuItem(name); if(key!=null)i.setAccelerator(key);i.addActionListener(e->action.accept(e));m.add(i); }

    private void addNewTab(){ EditorTab ed=new EditorTab(null); String title="새 파일"; tabs.addTab(title,ed.root); editors.put(ed.root,ed); tabs.setSelectedComponent(ed.root); ed.area.requestFocusInWindow(); }
    private EditorTab current(){ return editors.get(tabs.getSelectedComponent()); }
    private void chooseFile(){ JFileChooser c=new JFileChooser(); c.setFileFilter(new FileNameExtensionFilter("텍스트 및 Java 파일","txt","java","md","json","xml","properties","log")); if(c.showOpenDialog(frame)==JFileChooser.APPROVE_OPTION)openFile(c.getSelectedFile().toPath()); }
    private void openFile(Path p){ try { for(EditorTab e:editors.values())if(p.equals(e.path)){tabs.setSelectedComponent(e.root);return;} String s=Files.readString(p,StandardCharsets.UTF_8); EditorTab ed=new EditorTab(p);ed.area.setText(s);ed.dirty=false;ed.updateTitle();tabs.addTab(p.getFileName().toString(),ed.root);editors.put(ed.root,ed);tabs.setSelectedComponent(ed.root); }catch(IOException ex){error("파일을 열 수 없습니다: "+ex.getMessage());} }
    private void chooseWorkspace(){ JFileChooser c=new JFileChooser();c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(c.showOpenDialog(frame)==JFileChooser.APPROVE_OPTION){workspace=c.getSelectedFile().toPath();refreshTree();} }
    private void refreshTree(){ DefaultMutableTreeNode root=new DefaultMutableTreeNode(workspace.toFile());buildTree(root,workspace,0);fileTree.setModel(new DefaultTreeModel(root)); }
    private void buildTree(DefaultMutableTreeNode node,Path dir,int depth){ if(depth>5)return; File[] fs=dir.toFile().listFiles();if(fs==null)return;Arrays.sort(fs,Comparator.comparing((File f)->!f.isDirectory()).thenComparing(File::getName,String.CASE_INSENSITIVE_ORDER));for(File f:fs){if(f.getName().equals(".git")||f.getName().equals("target")||f.getName().startsWith("."))continue;DefaultMutableTreeNode ch=new DefaultMutableTreeNode(f);node.add(ch);if(f.isDirectory())buildTree(ch,f.toPath(),depth+1);} }

    private void saveCurrent(){EditorTab e=current();if(e==null)return;if(e.path==null){saveAsCurrent();return;}write(e);}
    private void saveAsCurrent(){EditorTab e=current();if(e==null)return;JFileChooser c=new JFileChooser();c.setSelectedFile(e.path==null?new File("untitled.txt"):e.path.toFile());if(c.showSaveDialog(frame)==JFileChooser.APPROVE_OPTION){e.path=c.getSelectedFile().toPath();write(e);}}
    private void write(EditorTab e){try{Path parent=e.path.toAbsolutePath().getParent();if(parent!=null)Files.createDirectories(parent);Files.writeString(e.path,e.area.getText(),StandardCharsets.UTF_8);e.dirty=false;e.updateTitle();appendTerminal("저장됨: "+e.path+"\n");}catch(IOException ex){error("저장 실패: "+ex.getMessage());}}
    private void saveAll(){for(EditorTab e:editors.values())if(e.dirty&&e.path!=null)write(e);}
    private boolean confirmCloseAll(){for(EditorTab e:editors.values())if(e.dirty){int r=JOptionPane.showConfirmDialog(frame,"저장되지 않은 변경 사항이 있습니다. 저장할까요?","CodeNote",JOptionPane.YES_NO_CANCEL_OPTION);if(r==JOptionPane.CANCEL_OPTION||r==JOptionPane.CLOSED_OPTION)return false;if(r==JOptionPane.YES_OPTION){tabs.setSelectedComponent(e.root);saveCurrent();}}return true;}
    private void showFind(){EditorTab e=current();if(e==null)return;String q=JOptionPane.showInputDialog(frame,"찾을 내용:");if(q==null||q.isEmpty())return;String text=e.area.getText();int start=Math.max(0,e.area.getCaretPosition());int at=text.indexOf(q,start);if(at<0)at=text.indexOf(q);if(at>=0){e.area.requestFocus();e.area.select(at,at+q.length());}else JOptionPane.showMessageDialog(frame,"검색 결과가 없습니다.");}

    private void compileAndRun(){EditorTab e=current();if(e==null)return;if(e.path==null){JOptionPane.showMessageDialog(frame,"먼저 Java 파일로 저장해 주세요.");saveAsCurrent();e=current();if(e==null||e.path==null)return;}if(!e.path.toString().endsWith(".java")){error(".java 파일만 실행할 수 있습니다.");return;}write(e);Path source=e.path.toAbsolutePath();Path out=source.getParent().resolve(".codenote-build");try{Files.createDirectories(out);String javac=javaTool("javac");ProcessBuilder pb=new ProcessBuilder(javac,"-encoding","UTF-8","-d",out.toString(),source.toString());pb.redirectErrorStream(true);Process p=pb.start();String compile=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);int code=p.waitFor();appendTerminal("$ javac "+source.getFileName()+"\n"+compile);if(code!=0){appendTerminal("컴파일 실패 (exit "+code+")\n");return;}String cls=source.getFileName().toString().replaceFirst("\\.java$","");appendTerminal("$ java "+cls+"\n");Process run=new ProcessBuilder(javaTool("java"),"-cp",out.toString(),cls).directory(source.getParent().toFile()).redirectErrorStream(true).start();new Thread(()->{try{String output=new String(run.getInputStream().readAllBytes(),StandardCharsets.UTF_8);int rc=run.waitFor();SwingUtilities.invokeLater(()->appendTerminal(output+"\n[프로그램 종료: "+rc+"]\n"));}catch(Exception ex){SwingUtilities.invokeLater(()->appendTerminal(ex.toString()+"\n"));}}).start();}catch(Exception ex){error("실행 오류: "+ex.getMessage());}}
    private String javaTool(String name){Path home=Paths.get(System.getProperty("java.home"),"bin",name+(isWindows()?".exe":""));return Files.exists(home)?home.toString():name;}
    private boolean isWindows(){return System.getProperty("os.name").toLowerCase().contains("win");}
    private void runShellCommand(){String cmd=commandField.getText().trim();if(cmd.isEmpty())return;commandField.setText("");appendTerminal("> "+cmd+"\n");try{ProcessBuilder pb=isWindows()?new ProcessBuilder("cmd.exe","/c",cmd):new ProcessBuilder("/bin/sh","-lc",cmd);if(workspace!=null)pb.directory(workspace.toFile());pb.redirectErrorStream(true);Process p=pb.start();new Thread(()->{try{String out=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);p.waitFor();SwingUtilities.invokeLater(()->appendTerminal(out+"\n"));}catch(Exception ex){SwingUtilities.invokeLater(()->appendTerminal(ex+"\n"));}}).start();}catch(IOException ex){appendTerminal(ex+"\n");}}
    private void appendTerminal(String s){if(terminal!=null){terminal.append(s);terminal.setCaretPosition(terminal.getDocument().getLength());}}
    private void error(String s){JOptionPane.showMessageDialog(frame,s,"오류",JOptionPane.ERROR_MESSAGE);}
    private void applyTheme(){Color bg=dark?DARK_BG:LIGHT_BG,panel=dark?DARK_PANEL:LIGHT_PANEL,fg=dark?DARK_FG:LIGHT_FG;applyThemeTo(frame,bg,panel,fg);for(EditorTab e:editors.values())e.applyTheme();terminal.setBackground(bg);terminal.setForeground(fg);commandField.setBackground(panel);commandField.setForeground(fg);frame.repaint();}
    private void applyThemeTo(Component c,Color bg,Color panel,Color fg){if(c instanceof JComponent jc){jc.setForeground(fg);jc.setBackground(c instanceof JPanel||c instanceof JTabbedPane||c instanceof JMenuBar?panel:bg);}if(c instanceof JMenuBar||c instanceof JMenu||c instanceof JMenuItem){c.setBackground(panel);c.setForeground(fg);}if(c instanceof Container co)for(Component ch:co.getComponents())applyThemeTo(ch,bg,panel,fg);}

    private class EditorTab{
        Path path; JPanel root; JTextPane area; JScrollPane scroll; JLabel status; javax.swing.Timer autoSave; boolean dirty=false; boolean updating=false;
        EditorTab(Path p){path=p;root=new JPanel(new BorderLayout());area=new JTextPane();area.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));LineNumbers lines=new LineNumbers(area);scroll=new JScrollPane(area);scroll.setRowHeaderView(lines);root.add(scroll,BorderLayout.CENTER);status=new JLabel("  Ln 1, Col 1     UTF-8");status.setBorder(new EmptyBorder(4,8,4,8));root.add(status,BorderLayout.SOUTH);
            area.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){changed();}public void removeUpdate(DocumentEvent e){changed();}public void changedUpdate(DocumentEvent e){}});
            area.addCaretListener(e->{int pos=area.getCaretPosition(),line=1,col=1;try{String t=area.getText(0,pos);line=(int)t.chars().filter(ch->ch=='\n').count()+1;int n=t.lastIndexOf('\n');col=pos-n;}catch(Exception ignored){}status.setText("  Ln "+line+", Col "+col+"     "+area.getText().length()+" chars     UTF-8");});
            autoSave=new javax.swing.Timer(1200,e->{if(dirty&&path!=null)writeQuietly();});autoSave.setRepeats(false);
            area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK),"save");area.getActionMap().put("save",new AbstractAction(){public void actionPerformed(ActionEvent e){saveCurrent();}});
            applyTheme();updateTitle();
        }
        void changed(){if(updating)return;dirty=true;updateTitle();autoSave.restart();highlight();}
        void writeQuietly(){try{Files.writeString(path,area.getText(),StandardCharsets.UTF_8);dirty=false;updateTitle();}catch(IOException ex){appendTerminal("자동 저장 실패: "+ex.getMessage()+"\n");}}
        void updateTitle(){int i=tabs.indexOfComponent(root);String name=path==null?"새 파일":path.getFileName().toString();if(i>=0)tabs.setTitleAt(i,(dirty?"● ":"")+name);}
        void applyTheme(){Color bg=dark?new Color(30,30,30):Color.WHITE,fg=dark?new Color(212,212,212):new Color(35,35,35);area.setBackground(bg);area.setForeground(fg);area.setCaretColor(fg);status.setBackground(dark?DARK_PANEL:LIGHT_PANEL);status.setForeground(fg);highlight();}
        void highlight(){SwingUtilities.invokeLater(()->{if(!area.isDisplayable()&&tabs.indexOfComponent(root)<0)return;String text=area.getText();StyledDocument doc=area.getStyledDocument();Style base=area.addStyle("base",null);StyleConstants.setForeground(base,dark?new Color(212,212,212):new Color(35,35,35));doc.setCharacterAttributes(0,doc.getLength(),base,true);if(!text.isEmpty()){colorPattern(doc,text,"\\b(public|private|protected|static|final|void|class|interface|enum|extends|implements|new|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|throws|import|package|this|super|var|instanceof)\\b",new Color(86,156,214));colorPattern(doc,text,"\\b(int|long|short|byte|double|float|boolean|char|String|Integer|Long|Object|System|Math|List|Map|Set)\\b",new Color(78,201,176));colorPattern(doc,text,"\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'",new Color(206,145,120));colorPattern(doc,text,"//[^\\n]*|/\\*[\\s\\S]*?\\*/",new Color(106,153,85));colorPattern(doc,text,"\\b\\d+(?:\\.\\d+)?\\b",new Color(181,206,168));}});}
        void colorPattern(StyledDocument d,String text,String regex,Color color){Matcher m=Pattern.compile(regex).matcher(text);Style s=area.addStyle("token"+color.getRGB(),null);StyleConstants.setForeground(s,color);while(m.find())d.setCharacterAttributes(m.start(),m.end()-m.start(),s,true);}
    }
    private class LineNumbers extends JComponent{JTextPane text;LineNumbers(JTextPane t){text=t;text.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){repaint();}public void removeUpdate(DocumentEvent e){repaint();}public void changedUpdate(DocumentEvent e){repaint();}});setPreferredSize(new Dimension(48,1));}protected void paintComponent(Graphics g){super.paintComponent(g);g.setColor(dark?new Color(45,45,45):new Color(245,245,245));g.fillRect(0,0,getWidth(),getHeight());g.setFont(text.getFont());g.setColor(dark?new Color(110,110,110):new Color(130,130,130));FontMetrics fm=g.getFontMetrics();Element root=text.getDocument().getDefaultRootElement();for(int i=0;i<root.getElementCount();i++){try{Rectangle r=text.modelToView2D(root.getElement(i).getStartOffset()).getBounds();String n=String.valueOf(i+1);g.drawString(n,getWidth()-fm.stringWidth(n)-10,r.y+fm.getAscent());}catch(Exception ignored){}}}}
}
