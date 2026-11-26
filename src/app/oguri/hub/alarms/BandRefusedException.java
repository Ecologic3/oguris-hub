package app.oguri.hub.alarms;

/**
 * The band answered a command with a non-OK result code (tag 0x7F)
 * instead of carrying it out. The known cause is a bond whose stored
 * key no longer matches the app's key: every plaintext step still
 * works, but each encrypted command is refused the same way. Kept
 * distinct from BandException so the transport can retry once with a
 * forced re-bond, which re-registers the app's key and heals it.
 */
public class BandRefusedException extends BandException {

    /** The band's result code (success is 0x000186A0). */
    public final int resultCode;

    public BandRefusedException(int resultCode) {
        super("Band refused the command (result " + resultCode + ")");
        this.resultCode = resultCode;
    }
}
