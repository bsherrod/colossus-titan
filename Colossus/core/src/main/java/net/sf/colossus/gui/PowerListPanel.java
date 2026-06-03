package net.sf.colossus.gui;


import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.PlayerPowers;


/**
 * Reusable UI component that displays a list of Lauer Power names
 * with mouseover tooltips showing descriptions.
 * Used in both the power selection dialog and the in-game status window.
 */
public class PowerListPanel extends JScrollPane
{
    private final DefaultListModel<String> model;
    private final JList<String> list;

    public PowerListPanel(int visibleRows)
    {
        model = new DefaultListModel<String>();
        list = new JList<String>(model);
        list.setVisibleRowCount(visibleRows);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Tooltip shows power description on hover
        list.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0)
                {
                    String name = model.get(index);
                    try
                    {
                        LauerPower power = LauerPower.valueOf(name);
                        list.setToolTipText("<html><b>" + power.getDisplayName()
                            + "</b><br><br>" + wrapText(power.getFullDescription(), 80)
                            + "</html>");
                    }
                    catch (IllegalArgumentException ex)
                    {
                        list.setToolTipText(name);
                    }
                }
                else
                {
                    list.setToolTipText(null);
                }
            }
        });

        setViewportView(list);
    }

    /** Add a power by enum name (e.g. "ARCHER"). No duplicates. */
    public void addPower(String enumName)
    {
        if (!model.contains(enumName))
        {
            model.addElement(enumName);
        }
    }

    /** Remove the currently selected power. */
    public void removeSelected()
    {
        int sel = list.getSelectedIndex();
        if (sel >= 0)
        {
            model.remove(sel);
        }
    }

    /** Set contents from a PlayerPowers object. */
    public void setPowers(PlayerPowers powers)
    {
        model.clear();
        if (powers != null)
        {
            for (LauerPower p : powers.getPowers())
            {
                model.addElement(p.name());
            }
        }
    }

    /** Get the underlying model for direct access. */
    public DefaultListModel<String> getModel()
    {
        return model;
    }

    /** Get the JList for selection access. */
    public JList<String> getList()
    {
        return list;
    }

    /** Serialize current contents to comma-separated enum names. */
    public String serialize()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < model.size(); i++)
        {
            if (sb.length() > 0)
            {
                sb.append(",");
            }
            sb.append(model.get(i));
        }
        return sb.toString();
    }

    /** Wrap text at word boundaries for HTML tooltip display. */
    private static String wrapText(String text, int width)
    {
        StringBuilder sb = new StringBuilder();
        int col = 0;
        for (String word : text.split(" "))
        {
            if (col + word.length() > width && col > 0)
            {
                sb.append("<br>");
                col = 0;
            }
            if (col > 0)
            {
                sb.append(" ");
                col++;
            }
            sb.append(word);
            col += word.length();
        }
        return sb.toString();
    }
}
