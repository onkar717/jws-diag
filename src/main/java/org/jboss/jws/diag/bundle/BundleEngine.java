package org.jboss.jws.diag.bundle;

import org.jboss.jws.diag.bundle.collect.FileCollector;
import org.jboss.jws.diag.bundle.model.CollectedFile;
import org.jboss.jws.diag.bundle.output.StagingWriter;
import org.jboss.jws.diag.bundle.redact.*;
import org.jboss.jws.diag.bundle.collect.LogCollector;
import org.jboss.jws.diag.common.RedactionLevel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BundleEngine {

    private final FileCollector fileCollector;
    private final LogCollector logCollector;
    private final StagingWriter stagingWriter;

    public BundleEngine() {
        this.fileCollector = new FileCollector();
        this.logCollector = new LogCollector();
        this.stagingWriter = new StagingWriter();
    }

    public void run(BundleContext context) throws IOException {
        List<CollectedFile> files = new ArrayList<>();

        files.addAll(fileCollector.collectConfFiles(context));
        files.addAll(logCollector.collectLogFiles(context));

        List<Redactor> redactors = buildRedactorChain(context.getRedactionLevel());

        for (CollectedFile file : files) {
            CollectedFile redactedFile;
            try {
                redactedFile = applyRedactors(file, redactors, context);
            } catch (RedactionException e) {
                System.err.println("[WARN] Skipping file due to redaction failure: "
                        + file.getRelativeArchivePath() + " (" + e.getMessage() + ")");
                context.recordSkippedFile();
                continue;
            }
            stagingWriter.write(redactedFile, context);
        }
    }

    private List<Redactor> buildRedactorChain(RedactionLevel level) {
        List<Redactor> chain = new ArrayList<>();
        chain.add(new XmlAttributeRedactor());
        chain.add(new PropertiesRedactor());
        chain.add(new LogRedactor());

        if (level == RedactionLevel.STRICT) {
            chain.add(new IpAddressRedactor());
            chain.add(new HostnameRedactor());
            chain.add(new EnvironmentVariableRedactor());
        }

        return chain;
    }

    private CollectedFile applyRedactors(CollectedFile file, List<Redactor> redactors, BundleContext context) {
        CollectedFile current = file;

        for (Redactor redactor : redactors) {
            if (redactor.supports(current)) {
                current = redactor.redact(current, context);
            }
        }

        return current;
    }
}