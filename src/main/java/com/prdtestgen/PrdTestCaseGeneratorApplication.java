package com.prdtestgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class PrdTestCaseGeneratorApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(PrdTestCaseGeneratorApplication.class, args);
	}

	/**
	 * Loads KEY=VALUE pairs from a .env file in the working directory into system
	 * properties, without overriding real environment variables or -D overrides.
	 * .env is gitignored — never commit it.
	 */
	private static void loadDotEnv() {
		Path envFile = Path.of(".env");
		if (!Files.isRegularFile(envFile)) {
			return;
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(envFile);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read .env file", e);
		}
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			int eq = trimmed.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String key = trimmed.substring(0, eq).trim();
			String value = trimmed.substring(eq + 1).trim();
			if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
				value = value.substring(1, value.length() - 1);
			}
			if (value.isBlank()) {
				continue;
			}
			if (System.getenv(key) == null && System.getProperty(key) == null) {
				System.setProperty(key, value);
			}
		}
	}

}
