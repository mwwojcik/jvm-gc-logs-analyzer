package pl.ks.profiling.safepoint.analyzer.standalone;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.ks.profiling.io.InputUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.StatsService;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;

@Slf4j
@SpringBootApplication
public class AnalyzerStandaloneApplication extends JFrame {
    @Autowired
    private StatsService statsService;

    @Autowired
    private PresentationFontProvider presentationFontProvider;

    public AnalyzerStandaloneApplication() {
    }

    public void init() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
        UIManager.getLookAndFeelDefaults()
                .put("defaultFont", presentationFontProvider.getDefaultFont());

        JButton quitButton = new JButton("Quit");
        JButton loadButton = new JButton("Load file");

        loadButton.addActionListener((ActionEvent event) -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                int ret = fileChooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile().getAbsoluteFile();
                    InputStream inputStream = InputUtils.getInputStream(file.getName(), file.getAbsolutePath());
                    SafepointLogFile stats = statsService.createAllStats(inputStream, file.getName());
                    new AnalyzerFrame(stats, presentationFontProvider);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        quitButton.addActionListener((ActionEvent event) -> {
            System.exit(0);
        });

        createLayout(loadButton, quitButton);

        setTitle("Safepoint/GC log file analyzer");
        setSize(300, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void createLayout(JComponent... arg) {
        setLayout(new FlowLayout());
        for (JComponent jComponent : arg) {
            add(jComponent);
        }
    }

    public static void main(String[] args) {
        var ctx = new SpringApplicationBuilder(AnalyzerStandaloneApplication.class)
                .headless(false).run(args);

        EventQueue.invokeLater(() -> {
            var ex = ctx.getBean(AnalyzerStandaloneApplication.class);
            try {
                ex.init();
            } catch (Exception e) {
                e.printStackTrace();
            }
            ex.setVisible(true);
        });
    }
}
