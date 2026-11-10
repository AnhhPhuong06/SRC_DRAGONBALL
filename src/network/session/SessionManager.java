package network.session;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import interfaces.ISession;

public class SessionManager {

    private static final SessionManager instance = new SessionManager();
    private final List<ISession> sessions = new CopyOnWriteArrayList<>();

    public static SessionManager gI() {
        return instance;
    }

    public SessionManager() {
    }

    public void putSession(ISession session) {
        if (session != null) {
            this.sessions.add(session);
        }
    }

    public void removeSession(ISession session) {
        if (session != null) {
            this.sessions.remove(session);
        }
    }

    public List<ISession> getSessions() {
        return this.sessions;
    }

    public ISession findByID(long id) throws Exception {
        for (ISession session : this.sessions) {
            if (session != null && session.getID() == id) {
                return session;
            }
        }
        throw new Exception("Session " + id + " does not exist");
    }

    public int getNumSession() {
        return this.sessions.size();
    }
}

