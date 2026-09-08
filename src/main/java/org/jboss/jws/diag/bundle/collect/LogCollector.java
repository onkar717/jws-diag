package org.jboss.jws.diag.bundle.collect;

import org.jboss.jws.diag.bundle.BundleContext;
import org.jboss.jws.diag.bundle.model.CollectedFile;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class LogCollector {

    private static final int MAX_LINES = 10_000;
    private static final int MAX_DAYS = 3;

    private static final String LOG_DIRECTORY = "logs";
    private static final String CATALINA_OUT = "catalina.out";

    private static final String[] LOG_PREFIXES = {
            "catalina",
            "localhost",
            "manager",
            "host-manager"
    };

    private static final Pattern LOG_LINE_DATE =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\s");

    private static final DateTimeFormatter LOG_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    private final Clock clock;

    public LogCollector() {
        this(Clock.systemDefaultZone());
    }

    LogCollector(Clock clock) {
        this.clock = clock;
    }

    public List<CollectedFile> collectLogFiles(BundleContext context) throws IOException {
        List<CollectedFile> files = new ArrayList<>();

        Path logsDir = context.getCatalinaBase().resolve(LOG_DIRECTORY);

        if (!Files.exists(logsDir)) {
            System.err.println(
                    "[WARN] Logs directory not found, skipping: " + logsDir);
            return files;
        }

        try (Stream<Path> stream = Files.list(logsDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isSupportedLogFile)
                    .forEach(path -> {
                        try {
                            String relativePath =
                                    LOG_DIRECTORY + "/" + path.getFileName();

                            boolean applyRetention =
                                    path.getFileName()
                                            .toString()
                                            .equalsIgnoreCase(CATALINA_OUT);

                            String content = applyRetention
                                    ? sliceCatalinaOut(path)
                                    : readEntireFile(path);

                            files.add(
                                    CollectedFile.builder()
                                            .relativeArchivePath(relativePath)
                                            .sourcePath(path)
                                            .type(CollectedFile.Type.LOG)
                                            .content(content)
                                            .build());

                        } catch (IOException e) {
                            System.err.println(
                                    "[WARN] Could not read log file: "
                                            + path + ": " + e.getMessage());
                            context.recordSkippedFile();
                        }
                    });
        }

        return files;
    }

    private boolean isSupportedLogFile(Path path) {
        String fileName =
                path.getFileName()
                        .toString()
                        .toLowerCase(Locale.ROOT);

        boolean supportedPrefix = false;

        for (String prefix : LOG_PREFIXES) {
            if (fileName.startsWith(prefix)) {
                supportedPrefix = true;
                break;
            }
        }

        if (!supportedPrefix) {
            return false;
        }

        return fileName.endsWith(".log")
                || fileName.equals(CATALINA_OUT);
    }

    private String sliceCatalinaOut(Path path) throws IOException {
        LocalDate today = LocalDate.now(clock);
        LocalDate windowStart = today.minusDays(MAX_DAYS - 1);

        ArrayDeque<String> kept = new ArrayDeque<>();

        List<String> pending = new ArrayList<>();

        try (TailLineReader reader = new TailLineReader(path)) {
            String line;

            while (kept.size() < MAX_LINES && (line = reader.previousLine()) != null) {
                Optional<LocalDate> headerDate = extractLineDate(line);

                if (headerDate.isEmpty()) {
                    pending.add(line);
                    continue;
                }

                LocalDate date = headerDate.get();

                if (date.isBefore(windowStart)) {
                    pending.clear();
                    break;
                }

                for (String p : pending) {
                    if (kept.size() >= MAX_LINES) {
                        break;
                    }
                    kept.addFirst(p);
                }
                pending.clear();

                if (kept.size() < MAX_LINES) {
                    kept.addFirst(line);
                }
            }
        }

        for (String p : pending) {
            if (kept.size() >= MAX_LINES) {
                break;
            }
            kept.addFirst(p);
        }

        return String.join(System.lineSeparator(), kept);
    }

    private String readEntireFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private Optional<LocalDate> extractLineDate(String line) {
        Matcher matcher = LOG_LINE_DATE.matcher(line);

        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    LocalDate.parse(
                            matcher.group(1),
                            LOG_DATE_FORMAT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static final class TailLineReader implements Closeable {

        private static final int CHUNK_SIZE = 64 * 1024;

        private final RandomAccessFile raf;
        private long position;
        private byte[] carry = new byte[0];
        private final ArrayDeque<byte[]> buffered = new ArrayDeque<>();
        private boolean exhausted;

        TailLineReader(Path path) throws IOException {
            this.raf = new RandomAccessFile(path.toFile(), "r");
            this.position = raf.length();
            this.exhausted = position == 0;
        }

        String previousLine() throws IOException {
            while (buffered.isEmpty() && !exhausted) {
                fillBuffer();
            }

            if (buffered.isEmpty()) {
                return null;
            }

            byte[] lineBytes = buffered.poll();
            String line = new String(lineBytes, StandardCharsets.UTF_8);

            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }

            return line;
        }

        private void fillBuffer() throws IOException {
            int size = (int) Math.min(CHUNK_SIZE, position);
            position -= size;

            byte[] block = new byte[size];
            raf.seek(position);
            raf.readFully(block);

            byte[] combined = new byte[block.length + carry.length];
            System.arraycopy(block, 0, combined, 0, block.length);
            System.arraycopy(carry, 0, combined, block.length, carry.length);

            int cursor = combined.length;
            List<byte[]> newestFirst = new ArrayList<>();

            for (int i = combined.length - 1; i >= 0; i--) {
                if (combined[i] == '\n') {
                    int lineStart = i + 1;

                    if (lineStart < cursor) {
                        byte[] lineBytes = new byte[cursor - lineStart];
                        System.arraycopy(combined, lineStart, lineBytes, 0, lineBytes.length);
                        newestFirst.add(lineBytes);
                    }

                    cursor = i;
                }
            }

            if (position == 0) {
                byte[] firstLine = new byte[cursor];
                System.arraycopy(combined, 0, firstLine, 0, cursor);

                if (firstLine.length > 0) {
                    newestFirst.add(firstLine);
                }

                carry = new byte[0];
                exhausted = true;
            } else {
                carry = new byte[cursor];
                System.arraycopy(combined, 0, carry, 0, cursor);
            }

            buffered.addAll(newestFirst);
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }
    }
}