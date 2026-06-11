package net.sf.colossus.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.Dice;

/**
 * Records all client→server commands to a JSON file for replay testing.
 * Always-on during gameplay. One file per game session.
 * 
 * Usage: GameRecorder.instance().record(method, args, playerName, game);
 * 
 * Output: ~/.colossus/recordings/game-<timestamp>.json
 */
public class GameRecorder
{
    private static final Logger LOGGER = Logger.getLogger(
        GameRecorder.class.getName());

    private static GameRecorder instance;
    private static String lastRecordingPath;

    public static String getLastRecordingPath()
    {
        return lastRecordingPath;
    }

    private BufferedWriter writer;
    private int seq;
    private boolean headerWritten;
    private final String filePath;
    private final List<Integer> diceBuffer = new ArrayList<Integer>();

    public static GameRecorder instance()
    {
        return instance;
    }

    public static void start(GameServerSide game)
    {
        if (instance != null)
        {
            instance.close();
        }
        instance = new GameRecorder(game);
    }

    public static void stop()
    {
        if (instance != null)
        {
            lastRecordingPath = instance.filePath;
            instance.close();
            instance = null;
        }
    }

    private GameRecorder(GameServerSide game)
    {
        String dir = System.getProperty("user.home") + File.separator
            + ".colossus" + File.separator + "recordings";
        new File(dir).mkdirs();
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss")
            .format(new Date());
        filePath = dir + File.separator + "game-" + timestamp + ".json";
        seq = 0;
        headerWritten = false;
        try
        {
            writer = new BufferedWriter(new FileWriter(filePath));
            writeHeader(game);
            Dice.setDiceListener(new Dice.DiceRollListener()
            {
                @Override
                public void onDiceRoll(int roll)
                {
                    diceBuffer.add(roll);
                }
            });
            LOGGER.info("GameRecorder started: " + filePath);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.WARNING, "Failed to start GameRecorder", e);
            writer = null;
        }
    }

    private void writeHeader(GameServerSide game)
    {
        try
        {
            writer.write("{\"header\":{");
            writer.write("\"version\":1,");
            writer.write("\"variant\":\"" + game.getVariant().getName() + "\",");
            writer.write("\"timestamp\":\"" + new Date() + "\",");
            writer.write("\"players\":[");
            java.util.Collection<? extends net.sf.colossus.game.Player> players = game.getPlayers();
            int i = 0; for (net.sf.colossus.game.Player p : players)
            {
                
                if (i > 0) writer.write(",");
                writer.write("{\"index\":" + i);
                writer.write(",\"name\":\"" + escape(p.getName()) + "\"");
                writer.write(",\"type\":\"" + p.getType() + "\"");
                writer.write(",\"powers\":\"" + p.getPowers().serialize() + "\"");
                writer.write("}"); i++;
            }
            writer.write("]},\"events\":[\n");
            headerWritten = true;
        }
        catch (IOException e)
        {
            LOGGER.log(Level.WARNING, "Failed to write header", e);
        }
    }

    public void record(String method, List<String> args, String playerName,
        GameServerSide game)
    {
        if (writer == null || !headerWritten)
        {
            return;
        }
        try
        {
            seq++;
            if (seq > 1) writer.write(",\n");
            writer.write("{\"seq\":" + seq);
            writer.write(",\"turn\":" + game.getTurnNumber());
            writer.write(",\"phase\":\"" + game.getPhase() + "\"");
            writer.write(",\"player\":\"" + escape(playerName) + "\"");
            writer.write(",\"method\":\"" + escape(method) + "\"");
            writer.write(",\"args\":[");
            for (int i = 0; i < args.size(); i++)
            {
                if (i > 0) writer.write(",");
                writer.write("\"" + escape(args.get(i)) + "\"");
            }
            writer.write("],\"dice\":[");
            for (int i = 0; i < diceBuffer.size(); i++)
            {
                if (i > 0) writer.write(",");
                writer.write(String.valueOf(diceBuffer.get(i)));
            }
            writer.write("]}");
            diceBuffer.clear();
            writer.flush();
        }
        catch (IOException e)
        {
            LOGGER.log(Level.WARNING, "Failed to record event", e);
        }
    }

    public void close()
    {
        if (writer != null)
        {
            try
            {
                writer.write("\n]}");
                writer.close();
                Dice.setDiceListener(null);
                LOGGER.info("GameRecorder closed: " + filePath
                    + " (" + seq + " events)");
            }
            catch (IOException e)
            {
                LOGGER.log(Level.WARNING, "Failed to close recorder", e);
            }
            writer = null;
        }
    }

    public String getFilePath()
    {
        return filePath;
    }

    private static String escape(String s)
    {
        if (s == null) return "null";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
