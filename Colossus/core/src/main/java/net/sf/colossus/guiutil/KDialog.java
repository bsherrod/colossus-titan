package net.sf.colossus.guiutil;


import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.util.InstanceTracker;


/**
 * KDialog adds some generally useful functions to JDialog.
 *
 * @author David Ripton
 */
@SuppressWarnings("serial")
public class KDialog extends JDialog
{
    private SaveWindow kSaveWindow;

    /**
     * Only support one of JDialog's many constructor forms.
     */
    public KDialog(Frame owner, String title, boolean modal)
    {
        super(owner, title, modal);
        InstanceTracker.register(this, "KDialog-for-?");
    }

    /**
     * Asserts that the current thread is the Event Dispatch Thread.
     *
     * @throws AssertionError if assertions are enabled and the current thread is
     *                        not the EDT
     */
    protected void assertEDT() throws AssertionError
    {
        assert SwingUtilities.isEventDispatchThread() : "GUI code should only run on the EDT";
    }

    /**
     * Place dialog relative to parentFrame's origin, offset by
     * point, and fully on-screen.
     */
    public void placeRelative(JFrame parentFrame, Point point, JScrollPane pane)
    {
        JViewport viewPort = pane.getViewport();

        // Absolute coordinate in the screen since the window is toplevel
        Point parentOrigin = parentFrame.getLocation();

        // Relative coordinate of the view, change when scrolling
        Point viewOrigin = viewPort.getViewPosition();

        Point origin = new Point(point.x + parentOrigin.x - viewOrigin.x,
            point.y + parentOrigin.y - viewOrigin.y);

        setLocation(origin);
    }

    /**
     * Center this dialog on the screen.
     *
     * Must be called after the dialog size has been set.
     */
    public void centerOnScreen()
    {
        // If we have an owner window, center over it instead of the
        // primary monitor. This handles multi-monitor setups correctly.
        java.awt.Window owner = getOwner();
        if (owner != null && owner.isVisible())
        {
            setLocationRelativeTo(owner);
        }
        else
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2));
        }
    }

    /**
     * Center this dialog on the screen, with an additional offset.
     *
     * Must be called after the dialog size has been set.
     */
    public void centerOnScreen(int xoffset, int yoffset)
    {
        java.awt.Window owner = getOwner();
        if (owner != null && owner.isVisible())
        {
            setLocationRelativeTo(owner);
            Point loc = getLocation();
            setLocation(loc.x + xoffset, loc.y + yoffset);
        }
        else
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(
                (d.width / 2 - getSize().width / 2) + xoffset,
                (d.height / 2 - getSize().height / 2) + yoffset));
        }
    }

    /**
     * Returns a point with a horizontal offset of the top right corner of the screen.
     *
     * This finds the upper right corner of the computer's screen and then moves
     * the location to the left by the given width.
     *
     * @param width The horizontal offset.
     * @return the target location
     */
    public Point getUpperRightCorner(int width)
    {
        java.awt.Window owner = getOwner();
        if (owner != null && owner.isVisible())
        {
            java.awt.Rectangle screen = owner.getGraphicsConfiguration().getBounds();
            return new Point(screen.x + screen.width - width, screen.y);
        }
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Point(d.width - width, 0);
    }

    // Move up a few pixels from the bottom, to help avoid taskbars.
    public void lowerRightCorner()
    {
        java.awt.Window owner = getOwner();
        if (owner != null && owner.isVisible())
        {
            java.awt.Rectangle screen = owner.getGraphicsConfiguration().getBounds();
            setLocation(new Point(screen.x + screen.width - getSize().width,
                screen.y + screen.height - getSize().height - 30));
        }
        else
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width - getSize().width, d.height
                - getSize().height - 30));
        }
    }

    /**
     * If, and only if, the extending class calls this useSaveWindow,
     * then the KDialog will handle the SaveWindow work:
     * creating it when useSaveWindow is called, and saving back
     * always when setVisible(false) is called (and useSaveWindow was
     * called before, of course).
     *
     * TODO maybe we should enforce this by calling it through the
     *      constructor
     *
     * @param options IOptions reference to the client for saving window
     *        size+pos in the Options data
     * @param windowName name/title of the window,
     *        window size+pos are stored for that name
     * @param defaultLocation to be used if no location was earlier stored:
     *        place there; give null to center on screen.
     */
    public void useSaveWindow(IOptions options, String windowName,
        Point defaultLocation)
    {
        kSaveWindow = new SaveWindow(options, windowName);
        if (defaultLocation == null)
        {
            kSaveWindow.restoreOrCenter(this);
        }
        else
        {
            kSaveWindow.restore(this, defaultLocation);
        }
    }

    @Override
    public void setVisible(boolean val)
    {
        if (!val && kSaveWindow != null)
        {
            kSaveWindow.save(this);
        }
        super.setVisible(val);
    }

    /**
     * Check if a point is on the same screen as this dialog's owner.
     * Returns true if no owner or can't determine (safe fallback).
     */
    public boolean isLocationOnOwnerScreen(Point location)
    {
        java.awt.Window owner = getOwner();
        if (owner == null || !owner.isVisible())
        {
            return true;
        }
        java.awt.GraphicsConfiguration gc = owner.getGraphicsConfiguration();
        if (gc == null)
        {
            return true;
        }
        return gc.getBounds().contains(location);
    }

    @Override
    public void dispose()
    {
        if (kSaveWindow != null)
        {
            kSaveWindow.save(this);
        }
        super.dispose();
        kSaveWindow = null;
    }
}
