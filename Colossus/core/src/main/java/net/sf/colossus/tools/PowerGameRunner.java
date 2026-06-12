package net.sf.colossus.tools;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.Dice;
import net.sf.colossus.server.GameServerSide;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


/**
 * Standalone runner: plays one full AI game with a specific power.
 * Usage: java -cp Colossus.jar net.sf.colossus.tools.PowerGameRunner POWER_NAME
 *
 * Exits 0 on success, 1 on error, 2 on no battles reached.
 */
public class PowerGameRunner
{
    private static final String COLOSSUS_HOME = "./core/src/functest/resource/colossus-home";

    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: PowerGameRunner <POWER_NAME>");
            System.exit(1);
        }
        String power = args[0];
        System.out.println("=== PowerGameRunner: " + power + " ===");

        Dice.captureSeed();
        Options.setStartupTest(false);
        Constants.END_AFTER_FIRST_BATTLE = false;
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        startOptions.setOption(Options.autoPickColor, true);
        startOptions.setOption(Options.autoPlay, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            COLOSSUS_HOME, false, true);
        serverOptions.loadOptions();
        serverOptions.setOption(Options.autoQuit, true);
        serverOptions.setOption(Options.autoStop, false);
        serverOptions.setOption(Options.playerName + "0", "Power0");
        serverOptions.setOption(Options.playerType + "0", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "0", power);
        serverOptions.setOption(Options.playerName + "1", "Opp1");
        serverOptions.setOption(Options.playerType + "1", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "1", "");
        serverOptions.setOption(Options.playerName + "2", "Opp2");
        serverOptions.setOption(Options.playerType + "2", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "2", "");

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);

        System.out.println("Starting game...");
        game.startNewGameAndWaitUntilOver(null);
        System.out.println("Game completed.");

        if (ErrorUtils.checkErrorDuringFunctionalTest())
        {
            System.err.println("SEVERE errors during " + power + " game!");
            System.exit(1);
        }

        System.out.println("=== " + power + ": PASS ===");
        System.exit(0);
    }
}
