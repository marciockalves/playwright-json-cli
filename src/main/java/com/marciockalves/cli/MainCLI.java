package com.marciockalves.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(
        name = "testcli",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Ferramenta CLI JSON-Driven para automacao E2E com Playwright e Java 21.",
        subcommands = {
                InitCommand.class,
                InteractiveCommand.class,
                RunCommand.class,
                ListCommand.class
        }
)
public class MainCLI implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MainCLI()).execute(args);
        System.exit(exitCode);
    }
}